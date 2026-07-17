import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class AiModelDto {
  const AiModelDto({
    required this.id,
    required this.displayName,
    required this.provider,
    required this.modelName,
    required this.kind,
    required this.enabled,
    required this.participatesInAuto,
    required this.supportsVision,
    required this.supportsText,
    required this.supportsJson,
    this.baseUrl,
    this.maskedApiKey,
  });

  factory AiModelDto.fromJson(JsonObject json) => AiModelDto(
        id: json['id']?.toString() ?? '',
        displayName: (json['displayName'] ?? json['name'])?.toString() ?? '',
        provider: json['provider']?.toString() ?? '',
        modelName: (json['modelName'] ?? json['model'])?.toString() ?? '',
        kind: (json['kind'] ?? json['scope'])?.toString() ?? 'USER',
        enabled: boolValue(json['enabled']),
        participatesInAuto: boolValue(json['participatesInAuto']),
        supportsVision: boolValue(json['supportsVision']),
        supportsText: json.containsKey('supportsText')
            ? boolValue(json['supportsText'])
            : true,
        supportsJson: boolValue(json['supportsJson']),
        baseUrl: json['baseUrl']?.toString(),
        maskedApiKey: json['maskedApiKey']?.toString(),
      );

  final String id;
  final String displayName;
  final String provider;
  final String modelName;
  final String kind;
  final bool enabled;
  final bool participatesInAuto;
  final bool supportsVision;
  final bool supportsText;
  final bool supportsJson;
  final String? baseUrl;
  final String? maskedApiKey;
}

class AiPurposePreferenceDto {
  const AiPurposePreferenceDto({
    required this.purpose,
    required this.mode,
    required this.allowUserModelsInAuto,
    required this.allowFallbackInManual,
    this.selectedModelId,
  });

  factory AiPurposePreferenceDto.fromJson(JsonObject json) =>
      AiPurposePreferenceDto(
        purpose: json['purpose']?.toString() ?? '',
        mode: json['mode']?.toString() ?? 'AUTO',
        selectedModelId: json['selectedModelId']?.toString(),
        allowUserModelsInAuto: json.containsKey('allowUserModelsInAuto')
            ? boolValue(json['allowUserModelsInAuto'])
            : true,
        allowFallbackInManual: boolValue(json['allowFallbackInManual']),
      );

  final String purpose;
  final String mode;
  final String? selectedModelId;
  final bool allowUserModelsInAuto;
  final bool allowFallbackInManual;
}

class AiModelDtoMapper {
  const AiModelDtoMapper();

  AiModelSummary modelFromDto(AiModelDto dto) => AiModelSummary(
        id: dto.id,
        displayName: dto.displayName,
        provider: dto.provider,
        modelName: dto.modelName,
        kind: dto.kind.toUpperCase() == 'SYSTEM'
            ? AiModelKind.system
            : AiModelKind.user,
        enabled: dto.enabled,
        participatesInAuto: dto.participatesInAuto,
        baseUrl: dto.baseUrl,
        maskedApiKey: dto.maskedApiKey,
        capabilities: AiModelCapabilities(
          supportsVision: dto.supportsVision,
          supportsText: dto.supportsText,
          supportsJson: dto.supportsJson,
        ),
      );

  AiPurposePreference preferenceFromDto(AiPurposePreferenceDto dto) {
    final purpose = AiPurpose.values.firstWhere(
      (value) => value.wireValue == dto.purpose.toUpperCase(),
      orElse: () => AiPurpose.visionAnalyze,
    );
    return AiPurposePreference(
      purpose: purpose,
      mode: dto.mode.toUpperCase() == 'MANUAL'
          ? AiSelectionMode.manual
          : AiSelectionMode.auto,
      selectedModelId: dto.selectedModelId,
      allowUserModelsInAuto: dto.allowUserModelsInAuto,
      allowFallbackInManual: dto.allowFallbackInManual,
    );
  }

  JsonObject createRequest(AiModelDraft draft, {bool includeEmptyKey = false}) => {
        'displayName': draft.displayName.trim(),
        'provider': draft.provider.trim(),
        'modelName': draft.modelName.trim(),
        if (draft.baseUrl?.trim().isNotEmpty == true) 'baseUrl': draft.baseUrl!.trim(),
        if (includeEmptyKey || draft.apiKey.trim().isNotEmpty) 'apiKey': draft.apiKey.trim(),
        'enabled': draft.enabled,
        'participatesInAuto': draft.participatesInAuto,
        'supportsVision': draft.capabilities.supportsVision,
        'supportsText': draft.capabilities.supportsText,
        'supportsJson': draft.capabilities.supportsJson,
      };

  JsonObject preferenceRequest(AiPurposePreference preference) => {
        'purpose': preference.purpose.wireValue,
        'mode': preference.mode == AiSelectionMode.auto ? 'AUTO' : 'MANUAL',
        if (preference.mode == AiSelectionMode.manual)
          'selectedModelId': preference.selectedModelId,
        'allowUserModelsInAuto': preference.allowUserModelsInAuto,
        'allowFallbackInManual': preference.allowFallbackInManual,
      };
}
