import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authApiServiceProvider = Provider<AuthApiService>((ref) {
  return AuthApiService(ref.watch(apiClientProvider));
});

class AuthApiService {
  const AuthApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<Map<String, Object?>> login({
    required Map<String, Object?> request,
  }) {
    return _apiClient.post<Map<String, Object?>>(
      '/auth/login',
      data: request,
    );
  }

  Future<Map<String, Object?>> register({
    required Map<String, Object?> request,
  }) {
    return _apiClient.post<Map<String, Object?>>(
      '/auth/register',
      data: request,
    );
  }

  Future<void> refreshSession() {
    return _apiClient.refreshSession();
  }

  Future<Map<String, Object?>> currentUser() {
    return _apiClient.get<Map<String, Object?>>('/users/me');
  }

  Future<Map<String, Object?>> updateProfile({
    required Map<String, Object?> request,
  }) {
    return _apiClient.patch<Map<String, Object?>>(
      '/users/me',
      data: request,
    );
  }

  Future<void> logout({Map<String, Object?>? request}) async {
    await _apiClient.post<Object?>(
      '/auth/logout',
      data: request,
    );
  }
}
