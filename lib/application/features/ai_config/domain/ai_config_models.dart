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
}

class AiModelCatalog {
  const AiModelCatalog({
    required this.systemModels,
    required this.userModels,
  });

  final List<AiModelSummary> systemModels;
  final List<AiModelSummary> userModels;

  List<AiModelSummary> get selectableModels => [
        ...systemModels.where((model) => model.enabled),
        ...userModels.where((model) => model.enabled),
      ];
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
