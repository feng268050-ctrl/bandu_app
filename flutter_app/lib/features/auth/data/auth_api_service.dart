import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authApiServiceProvider = Provider<AuthApiService>((ref) {
  return AuthApiService(ref.watch(apiClientProvider));
});

class AuthApiService {
  const AuthApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<Map<String, Object?>> login({
    required String email,
    required String password,
  }) {
    return _apiClient.post<Map<String, Object?>>(
      '/auth/login',
      data: {
        'email': email,
        'password': password,
      },
    );
  }

  Future<void> refreshSession() {
    return _apiClient.refreshSession();
  }

  Future<Map<String, Object?>> currentUser() {
    return _apiClient.get<Map<String, Object?>>('/users/me');
  }

  Future<void> logout() async {
    await _apiClient.post<Object?>('/auth/logout');
  }
}
