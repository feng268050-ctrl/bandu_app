import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_config_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/ai_config_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteAiConfigRepositoryProvider = Provider<AiConfigRepository>((ref) {
  return RemoteAiConfigRepository(
    apiService: ref.watch(aiConfigApiServiceProvider),
    mapper: const AiConfigDtoMapper(),
  );
});

class RemoteAiConfigRepository implements AiConfigRepository {
  const RemoteAiConfigRepository({
    required this.apiService,
    required this.mapper,
  });

  final AiConfigApiService apiService;
  final AiConfigDtoMapper mapper;

  @override
  Future<List<AiServiceConfig>> fetchConfigs() async {
    final dtos = await apiService.fetchConfigs();
    return dtos.map(mapper.configFromDto).toList();
  }

  @override
  Future<AiServiceConfig> createConfig(AiServiceConfigDraft draft) async {
    final dto = await apiService.createConfig(mapper.createRequest(draft));
    return mapper.configFromDto(dto);
  }

  @override
  Future<AiServiceConfig> updateConfig(
    String id,
    AiServiceConfigDraft draft,
  ) async {
    final dto = await apiService.updateConfig(id, mapper.updateRequest(draft));
    return mapper.configFromDto(dto);
  }

  @override
  Future<AiServiceConfig> setDefault(String id) async {
    final dto = await apiService.updateConfig(
      id,
      const UpdateAiConfigRequestDto(isDefault: true),
    );
    return mapper.configFromDto(dto);
  }

  @override
  Future<void> deleteConfig(String id) => apiService.deleteConfig(id);
}
