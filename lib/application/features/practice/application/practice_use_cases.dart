import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';

class GeneratePracticeUseCase {
  const GeneratePracticeUseCase(this._repository);

  final PracticeRepository _repository;

  Future<PracticeQuestion> call({
    required String errorItemId,
    String difficulty = 'medium',
  }) {
    return _repository.generate(
      errorItemId: errorItemId,
      difficulty: difficulty,
    );
  }
}

class RecordPracticeResultUseCase {
  const RecordPracticeResultUseCase(this._repository);

  final PracticeRepository _repository;

  Future<void> call({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) {
    return _repository.record(
      subject: subject,
      difficulty: difficulty,
      isCorrect: isCorrect,
    );
  }
}

class FetchPracticeHistoryUseCase {
  const FetchPracticeHistoryUseCase(this._repository);

  final PracticeRepository _repository;

  Future<List<PracticeRecord>> call({int limit = 10}) {
    return _repository.history(limit: limit);
  }
}
