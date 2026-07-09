import 'package:bandu_wrong_notebook/core/storage/token_storage.dart';
import 'package:bandu_wrong_notebook/features/auth/data/auth_api_service.dart';
import 'package:bandu_wrong_notebook/features/auth/data/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/features/auth/domain/auth_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return RemoteAuthRepository(
    apiService: ref.watch(authApiServiceProvider),
    tokenStore: ref.watch(tokenStoreProvider),
    mapper: const AuthDtoMapper(),
  );
});

class RemoteAuthRepository implements AuthRepository {
  const RemoteAuthRepository({
    required this.apiService,
    required this.tokenStore,
    required this.mapper,
  });

  final AuthApiService apiService;
  final TokenStore tokenStore;
  final AuthDtoMapper mapper;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final data = await apiService.login(email: email, password: password);
    final session = mapper.sessionFromJson(data);
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

    await apiService.refreshSession();
    final user = mapper.userFromJson(await apiService.currentUser());
    final accessToken = await tokenStore.readAccessToken();
    final nextRefreshToken = await tokenStore.readRefreshToken();

    if (accessToken == null || nextRefreshToken == null) {
      return null;
    }

    return AuthSession(
      user: user,
      accessToken: accessToken,
      refreshToken: nextRefreshToken,
    );
  }

  @override
  Future<void> logout() async {
    try {
      await apiService.logout();
    } finally {
      await tokenStore.clear();
    }
  }
}
