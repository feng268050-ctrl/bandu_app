import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/framework/network/services/auth_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/secure_storage/token_storage.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteAuthRepositoryProvider = Provider<AuthRepository>((ref) {
  return RemoteAuthRepository(
    apiService: ref.watch(authApiServiceProvider),
    tokenStore: ref.watch(secureTokenStoreProvider),
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
    final data = await apiService.login(
      request: mapper.loginRequest(email: email, password: password),
    );
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
  Future<AuthSession> register({
    required String email,
    required String password,
    String? name,
  }) async {
    final data = await apiService.register(
      request: mapper.registerRequest(
        email: email,
        password: password,
        name: name,
      ),
    );
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
  Future<UserProfile> updateProfile({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  }) async {
    final data = await apiService.updateProfile(
      request: mapper.updateProfileRequest(
        name: name,
        educationStage: educationStage,
        enrollmentYear: enrollmentYear,
      ),
    );
    return mapper.userFromJson(data);
  }

  @override
  Future<void> logout() async {
    final refreshToken = await tokenStore.readRefreshToken();
    try {
      await apiService.logout(
        request:
            refreshToken == null ? null : mapper.logoutRequest(refreshToken),
      );
    } finally {
      await tokenStore.clear();
    }
  }
}
