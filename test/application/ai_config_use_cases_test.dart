import 'package:bandu_wrong_notebook/application/features/ai_config/application/ai_config_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_repository.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const draft = AiServiceConfigDraft(
    name: '辅导模型',
    baseUrl: 'https://example.com/v1',
    apiKey: 'sk-secret',
    model: 'model-a',
    isDefault: true,
  );

  test('save use case selects create or update from the config id', () async {
    final repository = _FakeAiConfigRepository();
    final useCase = SaveAiConfigUseCase(repository);

    await useCase.call(draft: draft);
    await useCase.call(id: 'config-1', draft: draft);

    expect(repository.created, 1);
    expect(repository.updatedIds, ['config-1']);
  });

  test('default and delete use cases keep mutations explicit', () async {
    final repository = _FakeAiConfigRepository();

    await SetDefaultAiConfigUseCase(repository).call('config-1');
    await DeleteAiConfigUseCase(repository).call('config-2');

    expect(repository.defaultIds, ['config-1']);
    expect(repository.deletedIds, ['config-2']);
  });
}

class _FakeAiConfigRepository implements AiConfigRepository {
  int created = 0;
  final updatedIds = <String>[];
  final defaultIds = <String>[];
  final deletedIds = <String>[];

  @override
  Future<AiServiceConfig> createConfig(AiServiceConfigDraft draft) async {
    created += 1;
    return _config('created');
  }

  @override
  Future<void> deleteConfig(String id) async {
    deletedIds.add(id);
  }

  @override
  Future<List<AiServiceConfig>> fetchConfigs() async => [_config('config-1')];

  @override
  Future<AiServiceConfig> setDefault(String id) async {
    defaultIds.add(id);
    return _config(id);
  }

  @override
  Future<AiServiceConfig> updateConfig(
    String id,
    AiServiceConfigDraft draft,
  ) async {
    updatedIds.add(id);
    return _config(id);
  }

  AiServiceConfig _config(String id) {
    return AiServiceConfig(
      id: id,
      name: '辅导模型',
      baseUrl: 'https://example.com/v1',
      model: 'model-a',
      maskedApiKey: '••••••••',
      hasApiKey: true,
      isDefault: true,
    );
  }
}
