import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/application/ai_request_model_selection.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/practice_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remotePracticeRepositoryProvider = Provider<PracticeRepository>((ref) {
  return RemotePracticeRepository(
    apiService: ref.watch(practiceApiServiceProvider),
    mapper: const PracticeDtoMapper(),
    modelSelection: AiRequestModelSelection(
      ref.watch(aiPreferenceRepositoryProvider),
    ),
  );
});

class RemotePracticeRepository implements PracticeRepository {
  const RemotePracticeRepository({
    required this.apiService,
    required this.mapper,
    this.modelSelection,
  });

  final PracticeApiService apiService;
  final PracticeDtoMapper mapper;
  final AiRequestModelSelection? modelSelection;

  @override
  Future<PracticeQuestion> generate({
    required String errorItemId,
    String difficulty = 'medium',
  }) async {
    final dto = await apiService.generate(
      mapper.generateRequest(
        errorItemId: errorItemId,
        difficulty: difficulty,
        preference: await modelSelection?.forPurpose(
              AiPurpose.questionGenerate,
            ) ??
            const AiPurposePreference(purpose: AiPurpose.questionGenerate),
      ),
    );
    return mapper.questionFromDto(dto);
  }

  @override
  Future<void> record({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) {
    return apiService.record(
      mapper.recordRequest(
        subject: subject,
        difficulty: difficulty,
        isCorrect: isCorrect,
      ),
    );
  }

  @override
  Future<List<PracticeRecord>> history({int limit = 10}) async {
    return mapper.historyFromDtos(await apiService.history(limit: limit));
  }
}
