import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';

abstract interface class AiModelRepository {
  Future<AiModelCatalog> fetchCatalog();

  Future<AiModelSummary> createModel(AiModelDraft draft);

  Future<AiModelSummary> updateModel(String id, AiModelDraft draft);

  Future<AiModelSummary> setAvailability(
    String id, {
    required bool enabled,
    bool? participatesInAuto,
  });

  Future<void> deleteModel(String id);
}

abstract interface class AiPreferenceRepository {
  Future<List<AiPurposePreference>> fetchPreferences();

  Future<AiPurposePreference> savePreference(AiPurposePreference preference);
}
