import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_repository.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/application/ai_request_model_selection.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/api/question_bank/pdf_question_bank_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/question_bank_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/question_bank/local_question_bank_store.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final localQuestionBankStoreProvider = Provider<LocalQuestionBankStore>((ref) {
  return const LocalQuestionBankStore();
});

final frameworkQuestionBankRepositoryProvider =
    Provider<QuestionBankRepository>((ref) {
  return FrameworkQuestionBankRepository(
    apiService: ref.watch(questionBankApiServiceProvider),
    store: ref.watch(localQuestionBankStoreProvider),
    mapper: const PdfQuestionBankDtoMapper(),
    modelSelection: AiRequestModelSelection(
      ref.watch(aiPreferenceRepositoryProvider),
    ),
  );
});

class FrameworkQuestionBankRepository implements QuestionBankRepository {
  const FrameworkQuestionBankRepository({
    required this.apiService,
    required this.store,
    required this.mapper,
    required this.modelSelection,
  });

  final QuestionBankApiService apiService;
  final LocalQuestionBankStore store;
  final PdfQuestionBankDtoMapper mapper;
  final AiRequestModelSelection modelSelection;

  @override
  Future<String?> pickPdf() async {
    const pdfType = XTypeGroup(
      label: 'PDF',
      extensions: ['pdf'],
      mimeTypes: ['application/pdf'],
    );
    final result = await openFile(acceptedTypeGroups: const [pdfType]);
    return result?.path;
  }

  @override
  Future<PdfImportPreview> analyzePdf(
    String localPdfPath, {
    String? modelId,
  }) async {
    final dto = await apiService.analyzePdf(
      localPdfPath,
      modelId: modelId,
      preference: modelId == null
          ? await modelSelection.forPurpose(AiPurpose.pdfImport)
          : null,
    );
    return mapper.previewFromDto(dto, localPdfPath: localPdfPath);
  }

  @override
  Future<List<PdfQuestionSet>> loadQuestionSets() => store.load();

  @override
  Future<PdfQuestionSet> saveQuestionSet(PdfImportPreview preview) {
    return store.save(preview);
  }
}
