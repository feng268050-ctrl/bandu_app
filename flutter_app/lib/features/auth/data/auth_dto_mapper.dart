import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';

class AuthDtoMapper {
  const AuthDtoMapper();

  AuthSession sessionFromJson(Map<String, Object?> data) {
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

  UserProfile userFromJson(Map<String, Object?> data) {
    return UserProfile.fromJson(data);
  }
}
