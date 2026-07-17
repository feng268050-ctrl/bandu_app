import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class TutorModelDto {
  const TutorModelDto({
    required this.id,
    required this.name,
    required this.provider,
    required this.model,
    required this.isDefault,
  });

  factory TutorModelDto.fromJson(JsonObject json) {
    return TutorModelDto(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '默认模型',
      provider: json['provider']?.toString() ?? '',
      model: json['model']?.toString() ?? '',
      isDefault: boolValue(json['isDefault']),
    );
  }

  final String id;
  final String name;
  final String provider;
  final String model;
  final bool isDefault;
}

class TutorReplyDto {
  const TutorReplyDto({
    required this.id,
    required this.content,
    required this.modelId,
    required this.createdAt,
    this.resolvedModel,
  });

  factory TutorReplyDto.fromJson(JsonObject json) {
    return TutorReplyDto(
      id: json['id']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      modelId: json['modelId']?.toString() ?? '',
      createdAt: dateTimeValue(json['createdAt']),
      resolvedModel: _resolvedModel(json),
    );
  }

  final String id;
  final String content;
  final String modelId;
  final DateTime createdAt;
  final AiResolvedModel? resolvedModel;
}

class TutorHistoryMessageDto {
  const TutorHistoryMessageDto({required this.role, required this.content});

  final String role;
  final String content;

  JsonObject toJson() => {'role': role, 'content': content};
}

class TutorDtoMapper {
  const TutorDtoMapper();

  TutorModel modelFromDto(TutorModelDto dto) {
    return TutorModel(
      id: dto.id,
      name: dto.name,
      provider: dto.provider,
      model: dto.model,
      isDefault: dto.isDefault,
    );
  }

  TutorReply replyFromDto(TutorReplyDto dto) {
    return TutorReply(
      id: dto.id,
      content: dto.content,
      modelId: dto.modelId,
      createdAt: dto.createdAt,
      resolvedModel: dto.resolvedModel,
    );
  }

  TutorHistoryMessageDto historyToDto(TutorMessage message) {
    return TutorHistoryMessageDto(
      role: message.role == TutorMessageRole.user ? 'user' : 'assistant',
      content: message.content,
    );
  }
}

AiResolvedModel? _resolvedModel(JsonObject json) {
  final raw = json['resolvedModel'];
  if (raw is! JsonObject) return null;
  final id = raw['id']?.toString();
  final name = (raw['displayName'] ?? raw['name'] ?? raw['model'])?.toString();
  if (id == null || id.isEmpty || name == null || name.isEmpty) return null;
  return AiResolvedModel(
    id: id,
    displayName: name,
    fallbackOccurred: boolValue(
      json['fallbackOccurred'] ?? json['fallback'] ?? raw['fallbackOccurred'],
    ),
  );
}
