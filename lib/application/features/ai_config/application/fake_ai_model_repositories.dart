import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_model_repository.dart';

class FakeAiModelRepository implements AiModelRepository {
  FakeAiModelRepository({
    List<AiModelSummary>? systemModels,
    List<AiModelSummary>? userModels,
  })  : _systemModels = List.of(
          systemModels ??
              const [
                AiModelSummary(
                  id: 'system-vision',
                  displayName: '系统视觉模型',
                  provider: '',
                  modelName: 'vision',
                  kind: AiModelKind.system,
                  enabled: true,
                  participatesInAuto: true,
                  capabilities: AiModelCapabilities(
                    supportsVision: true,
                    supportsJson: true,
                  ),
                ),
                AiModelSummary(
                  id: 'system-text',
                  displayName: '系统文本模型',
                  provider: '',
                  modelName: 'text',
                  kind: AiModelKind.system,
                  enabled: true,
                  participatesInAuto: true,
                  capabilities: AiModelCapabilities(supportsJson: true),
                ),
              ],
        ),
        _userModels = List.of(userModels ?? const []);

  final List<AiModelSummary> _systemModels;
  final List<AiModelSummary> _userModels;
  int _nextId = 1;

  @override
  Future<AiModelCatalog> fetchCatalog() async => AiModelCatalog(
        systemModels: List.unmodifiable(_systemModels),
        userModels: List.unmodifiable(_userModels),
      );

  @override
  Future<AiModelSummary> createModel(AiModelDraft draft) async {
    if (draft.apiKey.trim().isEmpty) {
      throw ArgumentError.value(draft.apiKey, 'apiKey', 'API Key is required.');
    }
    final model = _fromDraft('user-${_nextId++}', draft);
    _userModels.add(model);
    return model;
  }

  @override
  Future<AiModelSummary> updateModel(String id, AiModelDraft draft) async {
    final userIndex = _userModels.indexWhere((model) => model.id == id);
    if (userIndex >= 0) {
      final updated = _fromDraft(
        id,
        draft,
        maskedApiKey: draft.apiKey.trim().isEmpty
            ? _userModels[userIndex].maskedApiKey
            : null,
      );
      _userModels[userIndex] = updated;
      return updated;
    }
    throw StateError('Only user models can be fully edited.');
  }

  @override
  Future<AiModelSummary> setAvailability(
    String id, {
    required bool enabled,
    bool? participatesInAuto,
  }) async {
    AiModelSummary patch(AiModelSummary model) => AiModelSummary(
          id: model.id,
          displayName: model.displayName,
          provider: model.provider,
          modelName: model.modelName,
          kind: model.kind,
          enabled: enabled,
          participatesInAuto: participatesInAuto ?? model.participatesInAuto,
          capabilities: model.capabilities,
          baseUrl: model.baseUrl,
          maskedApiKey: model.maskedApiKey,
        );

    final systemIndex = _systemModels.indexWhere((model) => model.id == id);
    if (systemIndex >= 0) {
      final updated = patch(_systemModels[systemIndex]);
      _systemModels[systemIndex] = updated;
      return updated;
    }
    final userIndex = _userModels.indexWhere((model) => model.id == id);
    if (userIndex >= 0) {
      final updated = patch(_userModels[userIndex]);
      _userModels[userIndex] = updated;
      return updated;
    }
    throw StateError('Model not found.');
  }

  @override
  Future<void> deleteModel(String id) async {
    final index = _userModels.indexWhere((model) => model.id == id);
    if (index < 0) throw StateError('System models cannot be deleted.');
    _userModels.removeAt(index);
  }

  AiModelSummary _fromDraft(
    String id,
    AiModelDraft draft, {
    String? maskedApiKey,
  }) {
    return AiModelSummary(
      id: id,
      displayName: draft.displayName.trim(),
      provider: draft.provider.trim(),
      modelName: draft.modelName.trim(),
      baseUrl: draft.baseUrl?.trim(),
      maskedApiKey: maskedApiKey ??
          '••••${draft.apiKey.trim().substring(draft.apiKey.trim().length - 1)}',
      kind: AiModelKind.user,
      enabled: draft.enabled,
      participatesInAuto: draft.participatesInAuto,
      capabilities: draft.capabilities,
    );
  }
}

class FakeAiPreferenceRepository implements AiPreferenceRepository {
  final Map<AiPurpose, AiPurposePreference> _preferences = {};

  @override
  Future<List<AiPurposePreference>> fetchPreferences() async => [
        for (final purpose in AiPurpose.values)
          _preferences[purpose] ?? AiPurposePreference(purpose: purpose),
      ];

  @override
  Future<AiPurposePreference> savePreference(
    AiPurposePreference preference,
  ) async {
    _preferences[preference.purpose] = preference;
    return preference;
  }
}
