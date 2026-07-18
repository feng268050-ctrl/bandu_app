import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';

abstract interface class QuestionBankRepository {
  Future<String?> pickPdf();

  Future<PdfImportPreview> analyzePdf(String localPdfPath, {String? modelId});

  Future<List<PdfQuestionSet>> loadQuestionSets();

  Future<PdfQuestionSet> saveQuestionSet(PdfImportPreview preview);

  Future<PdfQuestionSet> renameQuestionSet(String questionSetId, String name);

  Future<void> deleteQuestionSet(String questionSetId);

  Future<ExamSession> createExam(
    String questionSetId,
    Map<BankQuestionType, int> questionCounts,
  );

  Future<List<ExamSession>> loadExamSessions();

  Future<ExamSession?> loadExamSession(String sessionId);

  Future<ExamSession> renameExamSession(String sessionId, String title);

  Future<void> deleteExamSession(String sessionId);

  Future<ExamSession> submitExamAnswer(
    String sessionId,
    String attemptId,
    String userAnswer,
  );
}
