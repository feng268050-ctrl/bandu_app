import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemApiServiceProvider = Provider<ErrorItemApiService>((ref) {
  return ErrorItemApiService(ref.watch(apiClientProvider));
});

class ErrorItemApiService {
  const ErrorItemApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<Object?>> fetchErrorItems() {
    return _apiClient.get<List<Object?>>('/error-items');
  }

  Future<Map<String, Object?>> fetchErrorItem(String id) {
    return _apiClient.get<Map<String, Object?>>('/error-items/$id');
  }

  Future<Map<String, Object?>> updateErrorItem(
    String id,
    Map<String, Object?> data,
  ) {
    return _apiClient.patch<Map<String, Object?>>(
      '/error-items/$id',
      data: data,
    );
  }

  Future<void> deleteErrorItem(String id) async {
    await _apiClient.delete<Object?>('/error-items/$id');
  }
}
