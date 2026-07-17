class AiServiceConfig {
  const AiServiceConfig({
    required this.id,
    required this.name,
    required this.baseUrl,
    required this.model,
    required this.maskedApiKey,
    required this.hasApiKey,
    required this.isDefault,
  });

  final String id;
  final String name;
  final String baseUrl;
  final String model;
  final String maskedApiKey;
  final bool hasApiKey;
  final bool isDefault;
}

class AiServiceConfigDraft {
  const AiServiceConfigDraft({
    required this.name,
    required this.baseUrl,
    required this.apiKey,
    required this.model,
    required this.isDefault,
  });

  final String name;
  final String baseUrl;
  final String apiKey;
  final String model;
  final bool isDefault;
}

/// The semantic task passed to the server-side AI router.
enum AiPurpose {
  visionAnalyze,
  pdfImport,
  tutor,
  questionGenerate,
  answerExplain;

  String get wireValue => switch (this) {
        AiPurpose.visionAnalyze => 'VISION_ANALYZE',
        AiPurpose.pdfImport => 'PDF_IMPORT',
        AiPurpose.tutor => 'TUTOR',
        AiPurpose.questionGenerate => 'QUESTION_GENERATE',
        AiPurpose.answerExplain => 'ANSWER_EXPLAIN',
      };

  String get label => switch (this) {
        AiPurpose.visionAnalyze => '拍题分析',
        AiPurpose.pdfImport => 'PDF 导入',
        AiPurpose.tutor => 'AI 辅导',
        AiPurpose.questionGenerate => '出题',
        AiPurpose.answerExplain => '答案解析',
      };
}

enum AiModelKind { system, user }

enum AiSelectionMode { auto, manual }

class AiModelCapabilities {
  const AiModelCapabilities({
    this.supportsVision = false,
    this.supportsText = true,
    this.supportsJson = false,
  });

  final bool supportsVision;
  final bool supportsText;
  final bool supportsJson;
}

class AiModelSummary {
  const AiModelSummary({
    required this.id,
    required this.displayName,
    required this.provider,
    required this.modelName,
    required this.kind,
    required this.enabled,
    required this.participatesInAuto,
    required this.capabilities,
    this.baseUrl,
    this.maskedApiKey,
  });

  final String id;
  final String displayName;
  final String provider;
  final String modelName;
  final AiModelKind kind;
  final bool enabled;
  final bool participatesInAuto;
  final AiModelCapabilities capabilities;
  final String? baseUrl;
  final String? maskedApiKey;

  bool get isSystem => kind == AiModelKind.system;

  /// Station / relay label for UI (not the raw protocol id).
  String get stationLabel {
    final display = displayName.trim();
    final model = modelName.trim();
    if (display.contains('·')) {
      final left = display.split('·').first.trim();
      if (left.isNotEmpty) return left;
    }
    if (display.isNotEmpty &&
        display.toLowerCase() != model.toLowerCase() &&
        !display.toLowerCase().contains(model.toLowerCase())) {
      return display;
    }
    final host = _relayHost(baseUrl);
    if (host != null) return host;
    return _humanizeProvider(provider);
  }

  /// Prefer "modelName - station"; fall back to displayName.
  String get listLabel {
    final model = modelName.trim();
    final station = stationLabel.trim();
    if (model.isNotEmpty && station.isNotEmpty) return '$model - $station';
    if (model.isNotEmpty) return model;
    if (station.isNotEmpty) return station;
    return displayName.trim().isEmpty ? '未命名模型' : displayName.trim();
  }

  bool get isConfigured => modelName.trim().isNotEmpty;
}

String _humanizeProvider(String provider) {
  return switch (provider.trim().toLowerCase()) {
    'openai' => 'OpenAI',
    'gemini' => 'Gemini',
    'azure' => 'Azure',
    final value when value.isNotEmpty => provider.trim(),
    _ => '',
  };
}

String? _relayHost(String? baseUrl) {
  final raw = baseUrl?.trim();
  if (raw == null || raw.isEmpty) return null;
  final host = Uri.tryParse(raw)?.host;
  if (host == null || host.isEmpty) return null;
  const builtins = {
    'api.openai.com',
    'generativelanguage.googleapis.com',
    'openai.azure.com',
  };
  if (builtins.contains(host) || host.endsWith('.openai.azure.com')) {
    return null;
  }
  return host;
}

class AiModelCatalog {
  const AiModelCatalog({
    required this.systemModels,
    required this.userModels,
  });

  final List<AiModelSummary> systemModels;
  final List<AiModelSummary> userModels;

  List<AiModelSummary> get selectableModels => [
        ...systemModels.where((model) => model.enabled && model.isConfigured),
        ...userModels.where((model) => model.enabled && model.isConfigured),
      ];

  List<AiModelSummary> get configuredSystemModels =>
      systemModels.where((model) => model.isConfigured).toList(growable: false);

  List<AiModelSummary> get configuredUserModels =>
      userModels.where((model) => model.isConfigured).toList(growable: false);
}

class AiModelDraft {
  const AiModelDraft({
    required this.displayName,
    required this.provider,
    required this.modelName,
    required this.apiKey,
    required this.enabled,
    required this.participatesInAuto,
    required this.capabilities,
    this.baseUrl,
  });

  final String displayName;
  final String provider;
  final String modelName;
  /// Submitted only to the API; never stored in a domain model.
  final String apiKey;
  final bool enabled;
  final bool participatesInAuto;
  final AiModelCapabilities capabilities;
  final String? baseUrl;
}

class AiPurposePreference {
  const AiPurposePreference({
    required this.purpose,
    this.mode = AiSelectionMode.auto,
    this.selectedModelId,
    this.allowUserModelsInAuto = true,
    this.allowFallbackInManual = false,
  }) : assert(
          mode == AiSelectionMode.auto || selectedModelId != null,
          'Manual selection requires a model id.',
        );

  final AiPurpose purpose;
  final AiSelectionMode mode;
  final String? selectedModelId;
  final bool allowUserModelsInAuto;
  final bool allowFallbackInManual;

  Map<String, Object?> toRequestSelection() => {
        'mode': mode == AiSelectionMode.auto ? 'AUTO' : 'MANUAL',
        if (mode == AiSelectionMode.manual) 'modelId': selectedModelId,
      };
}

class AiResolvedModel {
  const AiResolvedModel({
    required this.id,
    required this.displayName,
    this.fallbackOccurred = false,
  });

  final String id;
  final String displayName;
  final bool fallbackOccurred;
}
