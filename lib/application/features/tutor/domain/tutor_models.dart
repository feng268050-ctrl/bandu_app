enum TutorMessageRole { user, assistant }

class TutorModel {
  const TutorModel({
    required this.id,
    required this.name,
    required this.provider,
    required this.model,
    this.isDefault = false,
  });

  final String id;
  final String name;
  final String provider;
  final String model;
  final bool isDefault;
}

class TutorQuestionContext {
  const TutorQuestionContext({
    required this.id,
    required this.title,
    required this.content,
    required this.source,
  });

  final String id;
  final String title;
  final String content;
  final String source;
}

class TutorMessage {
  const TutorMessage({
    required this.id,
    required this.role,
    required this.content,
    required this.createdAt,
    this.imagePath,
    this.questionContext,
  });

  final String id;
  final TutorMessageRole role;
  final String content;
  final DateTime createdAt;
  final String? imagePath;
  final TutorQuestionContext? questionContext;
}

class TutorSession {
  const TutorSession({
    required this.id,
    required this.title,
    required this.modelId,
    required this.messages,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final String? modelId;
  final List<TutorMessage> messages;
  final DateTime createdAt;
  final DateTime updatedAt;

  TutorSession copyWith({
    String? title,
    String? modelId,
    bool clearModel = false,
    List<TutorMessage>? messages,
    DateTime? updatedAt,
  }) {
    return TutorSession(
      id: id,
      title: title ?? this.title,
      modelId: clearModel ? null : modelId ?? this.modelId,
      messages: messages ?? this.messages,
      createdAt: createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

class TutorReply {
  const TutorReply({
    required this.id,
    required this.content,
    required this.modelId,
    required this.createdAt,
  });

  final String id;
  final String content;
  final String modelId;
  final DateTime createdAt;
}
