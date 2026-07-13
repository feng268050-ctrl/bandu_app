import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class TutorQuestionContextRecord {
  const TutorQuestionContextRecord({
    required this.id,
    required this.title,
    required this.content,
    required this.source,
  });

  factory TutorQuestionContextRecord.fromJson(JsonObject json) {
    return TutorQuestionContextRecord(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      source: json['source']?.toString() ?? '',
    );
  }

  final String id;
  final String title;
  final String content;
  final String source;

  JsonObject toJson() => {
        'id': id,
        'title': title,
        'content': content,
        'source': source,
      };
}

class TutorMessageRecord {
  const TutorMessageRecord({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
    this.imagePath,
    this.questionContext,
  });

  factory TutorMessageRecord.fromJson(JsonObject json) {
    final context = json['questionContext'];
    return TutorMessageRecord(
      id: json['id']?.toString() ?? '',
      role: json['role']?.toString() ?? 'user',
      content: json['content']?.toString() ?? '',
      createdAt: dateTimeValue(json['createdAt']),
      imagePath: json['imagePath']?.toString(),
      questionContext: context is Map
          ? TutorQuestionContextRecord.fromJson(
              requireJsonObject(context, context: 'tutor question context'),
            )
          : null,
    );
  }

  final String id;
  final String role;
  final String content;
  final DateTime createdAt;
  final String? imagePath;
  final TutorQuestionContextRecord? questionContext;

  JsonObject toJson() => {
        'id': id,
        'role': role,
        'content': content,
        'createdAt': createdAt.toIso8601String(),
        'imagePath': imagePath,
        'questionContext': questionContext?.toJson(),
      };
}

class TutorSessionRecord {
  const TutorSessionRecord({
    required this.id,
    required this.title,
    required this.messages,
    required this.createdAt,
    required this.updatedAt,
    this.modelId,
  });

  factory TutorSessionRecord.fromJson(JsonObject json) {
    final rawMessages = json['messages'];
    return TutorSessionRecord(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '新对话',
      modelId: json['modelId']?.toString(),
      messages: rawMessages is List
          ? rawMessages
              .map((item) => requireJsonObject(item, context: 'tutor message'))
              .map(TutorMessageRecord.fromJson)
              .toList()
          : const [],
      createdAt: dateTimeValue(json['createdAt']),
      updatedAt: dateTimeValue(json['updatedAt']),
    );
  }

  final String id;
  final String title;
  final String? modelId;
  final List<TutorMessageRecord> messages;
  final DateTime createdAt;
  final DateTime updatedAt;

  JsonObject toJson() => {
        'id': id,
        'title': title,
        'modelId': modelId,
        'messages': messages.map((message) => message.toJson()).toList(),
        'createdAt': createdAt.toIso8601String(),
        'updatedAt': updatedAt.toIso8601String(),
      };
}

class TutorSessionCacheMapper {
  const TutorSessionCacheMapper();

  TutorSession fromRecord(TutorSessionRecord record) {
    return TutorSession(
      id: record.id,
      title: record.title,
      modelId: record.modelId,
      messages: record.messages.map(_messageFromRecord).toList(),
      createdAt: record.createdAt,
      updatedAt: record.updatedAt,
    );
  }

  TutorSessionRecord toRecord(TutorSession session) {
    return TutorSessionRecord(
      id: session.id,
      title: session.title,
      modelId: session.modelId,
      messages: session.messages.map(_messageToRecord).toList(),
      createdAt: session.createdAt,
      updatedAt: session.updatedAt,
    );
  }

  TutorMessage _messageFromRecord(TutorMessageRecord record) {
    final context = record.questionContext;
    return TutorMessage(
      id: record.id,
      role: record.role == 'assistant'
          ? TutorMessageRole.assistant
          : TutorMessageRole.user,
      content: record.content,
      createdAt: record.createdAt,
      imagePath: record.imagePath,
      questionContext: context == null
          ? null
          : TutorQuestionContext(
              id: context.id,
              title: context.title,
              content: context.content,
              source: context.source,
            ),
    );
  }

  TutorMessageRecord _messageToRecord(TutorMessage message) {
    final context = message.questionContext;
    return TutorMessageRecord(
      id: message.id,
      role: message.role == TutorMessageRole.assistant ? 'assistant' : 'user',
      content: message.content,
      createdAt: message.createdAt,
      imagePath: message.imagePath,
      questionContext: context == null
          ? null
          : TutorQuestionContextRecord(
              id: context.id,
              title: context.title,
              content: context.content,
              source: context.source,
            ),
    );
  }
}
