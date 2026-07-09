import 'package:bandu_wrong_notebook/core/network/api_client.dart';
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
}
