import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_repositories.dart';

class LoadTutorModelsUseCase {
  const LoadTutorModelsUseCase(this._repository);
  final TutorRepository _repository;
  Future<List<TutorModel>> call() => _repository.fetchModels();
}

class LoadTutorSessionsUseCase {
  const LoadTutorSessionsUseCase(this._repository);
  final TutorRepository _repository;
  Future<List<TutorSession>> call() => _repository.loadSessions();
}

class SaveTutorSessionUseCase {
  const SaveTutorSessionUseCase(this._repository);
  final TutorRepository _repository;
  Future<void> call(TutorSession session) => _repository.saveSession(session);
}

class DeleteTutorSessionUseCase {
  const DeleteTutorSessionUseCase(this._repository);
  final TutorRepository _repository;
  Future<void> call(String sessionId) => _repository.deleteSession(sessionId);
}

class PickTutorImageUseCase {
  const PickTutorImageUseCase(this._repository);
  final TutorRepository _repository;
  Future<String?> call() => _repository.pickImage();
}

class SendTutorMessageUseCase {
  const SendTutorMessageUseCase(this._repository);
  final TutorRepository _repository;

  Future<TutorReply> call({
    required String message,
    required List<TutorMessage> history,
    String? modelId,
    TutorQuestionContext? questionContext,
    String? imagePath,
  }) {
    return _repository.sendMessage(
      message: message,
      history: history,
      modelId: modelId,
      questionContext: questionContext,
      imagePath: imagePath,
    );
  }
}

class StartSpeechInputUseCase {
  const StartSpeechInputUseCase(this._repository);
  final SpeechInputRepository _repository;

  Future<Stream<String>> call() async {
    final available = await _repository.initialize();
    if (!available) {
      throw StateError('当前设备无法使用语音输入');
    }
    return _repository.startListening();
  }
}

class StopSpeechInputUseCase {
  const StopSpeechInputUseCase(this._repository);
  final SpeechInputRepository _repository;
  Future<void> call() => _repository.stopListening();
}
