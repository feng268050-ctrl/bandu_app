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
