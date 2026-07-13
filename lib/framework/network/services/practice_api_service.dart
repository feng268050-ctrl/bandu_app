import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceApiServiceProvider = Provider<PracticeApiService>((ref) {
  return PracticeApiService(ref.watch(apiClientProvider));
});

class PracticeApiService {
  const PracticeApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<Map<String, Object?>> generate(Map<String, Object?> request) {
    return _apiClient.post<Map<String, Object?>>(
      '/practice/generate',
      data: request,
    );
  }

  Future<void> record(Map<String, Object?> request) async {
    await _apiClient.post<Object?>('/practice/submit', data: request);
  }

  Future<List<Object?>> history({int limit = 10}) {
    return _apiClient.get<List<Object?>>(
      '/practice/history',
      queryParameters: {'limit': limit},
    );
  }
}
