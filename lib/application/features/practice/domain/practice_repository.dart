import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';

abstract interface class PracticeRepository {
  Future<PracticeQuestion> generate({
    required String errorItemId,
    String difficulty = 'medium',
  });

  Future<void> record({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  });

  Future<List<PracticeRecord>> history({int limit = 10});
}
