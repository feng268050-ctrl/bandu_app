import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_model_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_model_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/ai_model_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteAiModelRepositoryProvider = Provider<AiModelRepository>((ref) {
  return RemoteAiModelRepository(
    apiService: ref.watch(aiModelApiServiceProvider),
    mapper: const AiModelDtoMapper(),
  );
});

final remoteAiPreferenceRepositoryProvider = Provider<AiPreferenceRepository>((ref) {
  return RemoteAiPreferenceRepository(
    apiService: ref.watch(aiModelApiServiceProvider),
    mapper: const AiModelDtoMapper(),
  );
});

class RemoteAiModelRepository implements AiModelRepository {
  const RemoteAiModelRepository({required this.apiService, required this.mapper});

  final AiModelApiService apiService;
  final AiModelDtoMapper mapper;

  @override
  Future<AiModelCatalog> fetchCatalog() async {
    final models = (await apiService.fetchModels()).map(mapper.modelFromDto).toList();
    return AiModelCatalog(
      systemModels: models.where((model) => model.kind == AiModelKind.system).toList(),
      userModels: models.where((model) => model.kind == AiModelKind.user).toList(),
    );
  }

  @override
  Future<AiModelSummary> createModel(AiModelDraft draft) async =>
      mapper.modelFromDto(await apiService.createModel(mapper.createRequest(draft)));

  @override
  Future<AiModelSummary> updateModel(String id, AiModelDraft draft) async =>
      mapper.modelFromDto(await apiService.updateModel(id, mapper.createRequest(draft)));

  @override
  Future<AiModelSummary> setAvailability(
    String id, {
    required bool enabled,
    bool? participatesInAuto,
  }) async =>
      mapper.modelFromDto(
        await apiService.updateModel(
          id,
          mapper.availabilityPatch(
            enabled: enabled,
            participatesInAuto: participatesInAuto,
          ),
        ),
      );

  @override
  Future<void> deleteModel(String id) => apiService.deleteModel(id);
}

class RemoteAiPreferenceRepository implements AiPreferenceRepository {
  const RemoteAiPreferenceRepository({
    required this.apiService,
    required this.mapper,
  });

  final AiModelApiService apiService;
  final AiModelDtoMapper mapper;

  @override
  Future<List<AiPurposePreference>> fetchPreferences() async =>
      (await apiService.fetchPreferences()).map(mapper.preferenceFromDto).toList();

  @override
  Future<AiPurposePreference> savePreference(AiPurposePreference preference) async =>
      mapper.preferenceFromDto(
        await apiService.savePreference(mapper.preferenceRequest(preference)),
      );
}
