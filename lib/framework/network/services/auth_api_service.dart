import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final authApiServiceProvider = Provider<AuthApiService>((ref) {
  return AuthApiService(ref.watch(apiClientProvider));
});

class AuthApiService {
  const AuthApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<AuthSessionDto> login(LoginRequestDto request) async {
    final payload = await _apiClient.post<Object?>(
      'auth/login',
      data: request.toJson(),
      timeouts: authRequestTimeouts,
    );
    return AuthSessionDto.fromJson(
      requireJsonObject(payload, context: 'login response'),
    );
  }

  Future<AuthSessionDto> register(RegisterRequestDto request) async {
    final payload = await _apiClient.post<Object?>(
      'auth/register',
      data: request.toJson(),
      timeouts: authRequestTimeouts,
    );
    return AuthSessionDto.fromJson(
      requireJsonObject(payload, context: 'register response'),
    );
  }

  Future<void> refreshSession() => _apiClient.refreshSession();

  Future<UserProfileDto> currentUser() async {
    final payload = await _apiClient.get<Object?>('users/me');
    return UserProfileDto.fromJson(
      requireJsonObject(payload, context: 'current user response'),
    );
  }

  Future<UserProfileDto> updateProfile(UpdateProfileRequestDto request) async {
    final payload = await _apiClient.patch<Object?>(
      'users/me',
      data: request.toJson(),
    );
    return UserProfileDto.fromJson(
      requireJsonObject(payload, context: 'update profile response'),
    );
  }

  Future<void> logout(LogoutRequestDto? request) async {
    await _apiClient.post<Object?>(
      'auth/logout',
      data: request?.toJson(),
      timeouts: authRequestTimeouts,
    );
  }
}
