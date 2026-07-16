import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_profile_store.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/auth_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/auth_api_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const cachedUser = UserProfile(
    id: 'user-1',
    email: 'student@example.com',
    name: '小伴',
  );

  test('login saves tokens and offline profile', () async {
    final tokens = _MemoryTokenStore();
    final profiles = _MemoryAuthProfileStore();
    final repository = RemoteAuthRepository(
      apiService: _FakeAuthApiService(),
      tokenStore: tokens,
      profileStore: profiles,
      mapper: const AuthDtoMapper(),
    );

    final session = await repository.login(
      email: 'student@example.com',
      password: 'password',
    );

    expect(session.user.id, 'user-1');
    expect(await tokens.readAccessToken(), 'access-token');
    expect(await tokens.readRefreshToken(), 'refresh-token');
    expect((await profiles.read())?.email, 'student@example.com');
  });

  test('network failure restores cached user for offline access', () async {
    final tokens = _MemoryTokenStore(
      const TokenPair(accessToken: '', refreshToken: 'refresh-token'),
    );
    final profiles = _MemoryAuthProfileStore(cachedUser);
    final repository = RemoteAuthRepository(
      apiService: _FakeAuthApiService(
        refreshError: const AppFailure(
          code: 'NETWORK_UNAVAILABLE',
          message: '当前网络不可用',
        ),
      ),
      tokenStore: tokens,
      profileStore: profiles,
      mapper: const AuthDtoMapper(),
    );

    final session = await repository.restoreSession();

    expect(session?.user, same(cachedUser));
    expect(session?.refreshToken, 'refresh-token');
    expect(tokens.clearCalls, 0);
    expect(profiles.clearCalls, 0);
  });

  test('authentication failure clears tokens and cached user', () async {
    final tokens = _MemoryTokenStore(
      const TokenPair(accessToken: '', refreshToken: 'expired-token'),
    );
    final profiles = _MemoryAuthProfileStore(cachedUser);
    final repository = RemoteAuthRepository(
      apiService: _FakeAuthApiService(
        refreshError: const AppFailure(
          code: 'HTTP_401',
          message: '登录状态已失效',
          statusCode: 401,
        ),
      ),
      tokenStore: tokens,
      profileStore: profiles,
      mapper: const AuthDtoMapper(),
    );

    await expectLater(repository.restoreSession(), throwsA(isA<AppFailure>()));

    expect(await tokens.readRefreshToken(), isNull);
    expect(await profiles.read(), isNull);
    expect(tokens.clearCalls, 1);
    expect(profiles.clearCalls, 1);
  });
}

class _FakeAuthApiService implements AuthApiService {
  _FakeAuthApiService({this.refreshError});

  final Object? refreshError;

  @override
  Future<UserProfileDto> currentUser() async {
    return const UserProfileDto(
      id: 'user-1',
      email: 'student@example.com',
      name: '小伴',
    );
  }

  @override
  Future<AuthSessionDto> login(LoginRequestDto request) async {
    return AuthSessionDto(
      user: UserProfileDto(
        id: 'user-1',
        email: request.email,
        name: '小伴',
      ),
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
    );
  }

  @override
  Future<void> logout(LogoutRequestDto? request) async {}

  @override
  Future<void> refreshSession() async {
    final error = refreshError;
    if (error != null) {
      throw error;
    }
  }

  @override
  Future<AuthSessionDto> register(RegisterRequestDto request) {
    return login(
        LoginRequestDto(email: request.email, password: request.password));
  }

  @override
  Future<UserProfileDto> updateProfile(UpdateProfileRequestDto request) async {
    return UserProfileDto(
      id: 'user-1',
      email: 'student@example.com',
      name: request.name,
      educationStage: request.educationStage,
      enrollmentYear: request.enrollmentYear,
    );
  }
}

class _MemoryTokenStore implements TokenStore {
  _MemoryTokenStore([this.tokens]);

  TokenPair? tokens;
  int clearCalls = 0;

  @override
  Future<void> clear() async {
    clearCalls += 1;
    tokens = null;
  }

  @override
  Future<String?> readAccessToken() async => tokens?.accessToken;

  @override
  Future<String?> readRefreshToken() async => tokens?.refreshToken;

  @override
  Future<void> save(TokenPair tokenPair) async {
    tokens = tokenPair;
  }
}

class _MemoryAuthProfileStore implements AuthProfileStore {
  _MemoryAuthProfileStore([this.profile]);

  UserProfile? profile;
  int clearCalls = 0;

  @override
  Future<void> clear() async {
    clearCalls += 1;
    profile = null;
  }

  @override
  Future<UserProfile?> read() async => profile;

  @override
  Future<void> save(UserProfile value) async {
    profile = value;
  }
}
