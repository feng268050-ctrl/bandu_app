import 'package:bandu_wrong_notebook/application/features/ai_config/application/fake_ai_model_repositories.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const draft = AiModelDraft(
    displayName: '我的模型',
    provider: 'Compatible',
    modelName: 'model-a',
    apiKey: 'sk-secret',
    enabled: true,
    participatesInAuto: true,
    capabilities: AiModelCapabilities(supportsJson: true),
  );

  test('fake model repository masks keys and rejects system deletion', () async {
    final repository = FakeAiModelRepository();
    final created = await repository.createModel(draft);

    expect(created.maskedApiKey, isNot(contains('sk-secret')));
    expect((await repository.fetchCatalog()).userModels, hasLength(1));
    await expectLater(
      repository.deleteModel('system-vision'),
      throwsA(isA<StateError>()),
    );
  });

  test('preferences default to Auto and persist per purpose', () async {
    final repository = FakeAiPreferenceRepository();
    expect(
      (await repository.fetchPreferences())
          .singleWhere((item) => item.purpose == AiPurpose.tutor)
          .mode,
      AiSelectionMode.auto,
    );
    await repository.savePreference(const AiPurposePreference(
      purpose: AiPurpose.tutor,
      mode: AiSelectionMode.manual,
      selectedModelId: 'user-1',
    ));
    expect(
      (await repository.fetchPreferences())
          .singleWhere((item) => item.purpose == AiPurpose.tutor)
          .selectedModelId,
      'user-1',
    );
  });
}
