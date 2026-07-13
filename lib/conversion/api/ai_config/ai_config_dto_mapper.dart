import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class AiServiceConfigDto {
  const AiServiceConfigDto({
    required this.id,
    required this.name,
    required this.baseUrl,
    required this.model,
    required this.maskedApiKey,
    required this.hasApiKey,
    required this.isDefault,
  });

  factory AiServiceConfigDto.fromJson(JsonObject json) {
    return AiServiceConfigDto(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      baseUrl: json['baseUrl']?.toString() ?? '',
      model: json['model']?.toString() ?? '',
      maskedApiKey: json['maskedApiKey']?.toString() ?? '',
      hasApiKey: boolValue(json['hasApiKey']),
      isDefault: boolValue(json['isDefault']),
    );
  }

  final String id;
  final String name;
  final String baseUrl;
  final String model;
  final String maskedApiKey;
  final bool hasApiKey;
  final bool isDefault;
}

class CreateAiConfigRequestDto {
  const CreateAiConfigRequestDto({
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

  JsonObject toJson() => {
        'name': name,
        'baseUrl': baseUrl,
        'apiKey': apiKey,
        'model': model,
        'isDefault': isDefault,
      };
}

class UpdateAiConfigRequestDto {
  const UpdateAiConfigRequestDto({
    this.name,
    this.baseUrl,
    this.apiKey,
    this.model,
    this.isDefault,
  });

  final String? name;
  final String? baseUrl;
  final String? apiKey;
  final String? model;
  final bool? isDefault;

  JsonObject toJson() => {
        if (name != null) 'name': name,
        if (baseUrl != null) 'baseUrl': baseUrl,
        if (apiKey != null && apiKey!.isNotEmpty) 'apiKey': apiKey,
        if (model != null) 'model': model,
        if (isDefault != null) 'isDefault': isDefault,
      };
}

class AiConfigDtoMapper {
  const AiConfigDtoMapper();

  AiServiceConfig configFromDto(AiServiceConfigDto dto) {
    return AiServiceConfig(
      id: dto.id,
      name: dto.name,
      baseUrl: dto.baseUrl,
      model: dto.model,
      maskedApiKey: dto.maskedApiKey,
      hasApiKey: dto.hasApiKey,
      isDefault: dto.isDefault,
    );
  }

  CreateAiConfigRequestDto createRequest(AiServiceConfigDraft draft) {
    return CreateAiConfigRequestDto(
      name: draft.name.trim(),
      baseUrl: draft.baseUrl.trim(),
      apiKey: draft.apiKey.trim(),
      model: draft.model.trim(),
      isDefault: draft.isDefault,
    );
  }

  UpdateAiConfigRequestDto updateRequest(AiServiceConfigDraft draft) {
    return UpdateAiConfigRequestDto(
      name: draft.name.trim(),
      baseUrl: draft.baseUrl.trim(),
      apiKey: draft.apiKey.trim().isEmpty ? null : draft.apiKey.trim(),
      model: draft.model.trim(),
      isDefault: draft.isDefault,
    );
  }
}
