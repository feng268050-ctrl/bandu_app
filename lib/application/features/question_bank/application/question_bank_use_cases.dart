import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_repository.dart';

class PickPdfQuestionSetUseCase {
  const PickPdfQuestionSetUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<String?> call() => _repository.pickPdf();
}

class AnalyzePdfQuestionSetUseCase {
  const AnalyzePdfQuestionSetUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<PdfImportPreview> call(String path, {String? modelId}) {
    return _repository.analyzePdf(path, modelId: modelId);
  }
}

class LoadPdfQuestionSetsUseCase {
  const LoadPdfQuestionSetsUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<List<PdfQuestionSet>> call() => _repository.loadQuestionSets();
}

class SavePdfQuestionSetUseCase {
  const SavePdfQuestionSetUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<PdfQuestionSet> call(PdfImportPreview preview) {
    return _repository.saveQuestionSet(preview);
  }
}

class RenamePdfQuestionSetUseCase {
  const RenamePdfQuestionSetUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<PdfQuestionSet> call({
    required String questionSetId,
    required String name,
  }) {
    return _repository.renameQuestionSet(questionSetId, name);
  }
}

class DeletePdfQuestionSetUseCase {
  const DeletePdfQuestionSetUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<void> call(String questionSetId) {
    return _repository.deleteQuestionSet(questionSetId);
  }
}

class CreateRandomExamUseCase {
  const CreateRandomExamUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<ExamSession> call({
    required String questionSetId,
    required Map<BankQuestionType, int> questionCounts,
  }) {
    return _repository.createExam(questionSetId, questionCounts);
  }
}

class LoadExamSessionUseCase {
  const LoadExamSessionUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<ExamSession?> call(String sessionId) {
    return _repository.loadExamSession(sessionId);
  }
}

class LoadExamSessionsUseCase {
  const LoadExamSessionsUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<List<ExamSession>> call() => _repository.loadExamSessions();
}

class RenameExamSessionUseCase {
  const RenameExamSessionUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<ExamSession> call({
    required String sessionId,
    required String title,
  }) {
    return _repository.renameExamSession(sessionId, title);
  }
}

class DeleteExamSessionUseCase {
  const DeleteExamSessionUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<void> call(String sessionId) {
    return _repository.deleteExamSession(sessionId);
  }
}

class SubmitExamAnswerUseCase {
  const SubmitExamAnswerUseCase(this._repository);

  final QuestionBankRepository _repository;

  Future<ExamSession> call({
    required String sessionId,
    required String attemptId,
    required String userAnswer,
  }) {
    return _repository.submitExamAnswer(sessionId, attemptId, userAnswer);
  }
}
