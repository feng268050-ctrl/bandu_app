import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';

abstract interface class QuestionBankRepository {
  Future<String?> pickPdf();

  Future<PdfImportPreview> analyzePdf(String localPdfPath, {String? modelId});

  Future<List<PdfQuestionSet>> loadQuestionSets();

  Future<PdfQuestionSet> saveQuestionSet(PdfImportPreview preview);
}
