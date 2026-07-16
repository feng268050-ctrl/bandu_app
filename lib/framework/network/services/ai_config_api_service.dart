import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_config_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final aiConfigApiServiceProvider = Provider<AiConfigApiService>((ref) {
  return AiConfigApiService(ref.watch(apiClientProvider));
});

class AiConfigApiService {
  const AiConfigApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AiServiceConfigDto>> fetchConfigs() async {
    final payload = await _apiClient.get<Object?>('ai-configs');
    return requireJsonArray(payload, context: 'AI configs response')
        .map((item) => requireJsonObject(item, context: 'AI config'))
        .map(AiServiceConfigDto.fromJson)
        .toList();
  }

  Future<AiServiceConfigDto> createConfig(
    CreateAiConfigRequestDto request,
  ) async {
    final payload = await _apiClient.post<Object?>(
      'ai-configs',
      data: request.toJson(),
    );
    return AiServiceConfigDto.fromJson(
      requireJsonObject(payload, context: 'create AI config response'),
    );
  }

  Future<AiServiceConfigDto> updateConfig(
    String id,
    UpdateAiConfigRequestDto request,
  ) async {
    final payload = await _apiClient.patch<Object?>(
      'ai-configs/$id',
      data: request.toJson(),
    );
    return AiServiceConfigDto.fromJson(
      requireJsonObject(payload, context: 'update AI config response'),
    );
  }

  Future<void> deleteConfig(String id) async {
    await _apiClient.delete<Object?>('ai-configs/$id');
  }
}
