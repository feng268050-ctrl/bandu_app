import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/core/storage/token_storage.dart';
import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return RemoteAuthRepository(
    apiClient: ref.watch(apiClientProvider),
    tokenStore: ref.watch(tokenStoreProvider),
  );
});

abstract interface class AuthRepository {
  Future<AuthSession> login({
    required String email,
    required String password,
  });

  Future<AuthSession?> restoreSession();

  Future<void> logout();
}

class RemoteAuthRepository implements AuthRepository {
  const RemoteAuthRepository({
    required this.apiClient,
    required this.tokenStore,
  });

  final ApiClient apiClient;
  final TokenStore tokenStore;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final data = await apiClient.post<Map<String, Object?>>(
      '/auth/login',
      data: {
        'email': email,
        'password': password,
      },
    );
    final session = _parseSession(data);
    await tokenStore.save(
      TokenPair(
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
      ),
    );
    return session;
  }

  @override
  Future<AuthSession?> restoreSession() async {
    final refreshToken = await tokenStore.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return null;
    }

    await apiClient.refreshSession();
    final user = await apiClient.get<Map<String, Object?>>('/users/me');
    final accessToken = await tokenStore.readAccessToken();
    final nextRefreshToken = await tokenStore.readRefreshToken();

    if (accessToken == null || nextRefreshToken == null) {
      return null;
    }

    return AuthSession(
      user: UserProfile.fromJson(user),
      accessToken: accessToken,
      refreshToken: nextRefreshToken,
    );
  }

  @override
  Future<void> logout() async {
    try {
      await apiClient.post<Object?>('/auth/logout');
    } finally {
      await tokenStore.clear();
    }
  }

  AuthSession _parseSession(Map<String, Object?> data) {
    final userPayload = data['user'];
    if (userPayload is! Map<String, Object?>) {
      throw StateError('登录响应缺少 user');
    }

    return AuthSession(
      user: UserProfile.fromJson(userPayload),
      accessToken: data['accessToken']?.toString() ?? '',
      refreshToken: data['refreshToken']?.toString() ?? '',
    );
  }
}
