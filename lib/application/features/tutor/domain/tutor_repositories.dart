import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';

abstract interface class TutorRepository {
  Future<List<TutorModel>> fetchModels();

  Future<List<TutorSession>> loadSessions();

  Future<void> saveSession(TutorSession session);

  Future<void> deleteSession(String sessionId);

  Future<String?> pickImage();

  Future<TutorReply> sendMessage({
    required String message,
    required List<TutorMessage> history,
    String? modelId,
    TutorQuestionContext? questionContext,
    String? imagePath,
  });
}

abstract interface class SpeechInputRepository {
  Future<bool> initialize();

  Stream<String> startListening();

  Future<void> stopListening();
}
