import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_model_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final aiModelApiServiceProvider = Provider<AiModelApiService>((ref) {
  return AiModelApiService(ref.watch(apiClientProvider));
});

class AiModelApiService {
  const AiModelApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AiModelDto>> fetchModels() async {
    final payload = await _apiClient.get<Object?>('ai/models');
    final objects = payload is Map<String, Object?> && payload['models'] is List
        ? requireJsonArray(payload['models'], context: 'AI models')
        : requireJsonArray(payload, context: 'AI models');
    return objects
        .map((item) => AiModelDto.fromJson(requireJsonObject(item, context: 'AI model')))
        .toList();
  }

  Future<AiModelDto> createModel(JsonObject request) async {
    final payload = await _apiClient.post<Object?>('ai/models', data: request);
    return AiModelDto.fromJson(requireJsonObject(payload, context: 'create AI model'));
  }

  Future<AiModelDto> updateModel(String id, JsonObject request) async {
    final payload = await _apiClient.patch<Object?>('ai/models/$id', data: request);
    return AiModelDto.fromJson(requireJsonObject(payload, context: 'update AI model'));
  }

  Future<void> deleteModel(String id) => _apiClient.delete<Object?>('ai/models/$id');

  Future<List<AiPurposePreferenceDto>> fetchPreferences() async {
    final payload = await _apiClient.get<Object?>('ai/preferences');
    final objects =
        payload is Map<String, Object?> && payload['preferences'] is List
            ? requireJsonArray(payload['preferences'], context: 'AI preferences')
            : requireJsonArray(payload, context: 'AI preferences');
    return objects
        .map((item) => AiPurposePreferenceDto.fromJson(
              requireJsonObject(item, context: 'AI preference'),
            ))
        .toList();
  }

  Future<AiPurposePreferenceDto> savePreference(
    JsonObject request,
  ) async {
    final payload = await _apiClient.put<Object?>('ai/preferences', data: request);
    return AiPurposePreferenceDto.fromJson(
      requireJsonObject(payload, context: 'save AI preference'),
    );
  }
}
