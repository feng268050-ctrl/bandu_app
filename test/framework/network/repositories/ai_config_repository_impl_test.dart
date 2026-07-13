import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_config_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/ai_config_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/ai_config_api_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('AI config repository maps typed requests and responses', () async {
    final service = _FakeAiConfigApiService();
    final repository = RemoteAiConfigRepository(
      apiService: service,
      mapper: const AiConfigDtoMapper(),
    );
    const draft = AiServiceConfigDraft(
      name: ' 辅导模型 ',
      baseUrl: ' https://example.com/v1 ',
      apiKey: '',
      model: ' model-a ',
      isDefault: false,
    );

    final configs = await repository.fetchConfigs();
    await repository.updateConfig('config-1', draft);
    await repository.setDefault('config-1');
    await repository.deleteConfig('config-2');

    expect(configs.single.id, 'config-1');
    expect(service.updateRequests.first.toJson(), {
      'name': '辅导模型',
      'baseUrl': 'https://example.com/v1',
      'model': 'model-a',
      'isDefault': false,
    });
    expect(service.updateRequests.last.toJson(), {'isDefault': true});
    expect(service.deletedIds, ['config-2']);
  });
}

class _FakeAiConfigApiService implements AiConfigApiService {
  final updateRequests = <UpdateAiConfigRequestDto>[];
  final deletedIds = <String>[];

  @override
  Future<AiServiceConfigDto> createConfig(
    CreateAiConfigRequestDto request,
  ) async {
    return _dto;
  }

  @override
  Future<void> deleteConfig(String id) async {
    deletedIds.add(id);
  }

  @override
  Future<List<AiServiceConfigDto>> fetchConfigs() async => [_dto];

  @override
  Future<AiServiceConfigDto> updateConfig(
    String id,
    UpdateAiConfigRequestDto request,
  ) async {
    updateRequests.add(request);
    return _dto;
  }

  AiServiceConfigDto get _dto => const AiServiceConfigDto(
        id: 'config-1',
        name: '辅导模型',
        baseUrl: 'https://example.com/v1',
        model: 'model-a',
        maskedApiKey: '••••••••',
        hasApiKey: true,
        isDefault: true,
      );
}
