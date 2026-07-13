import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class UserProfileDto {
  const UserProfileDto({
    required this.id,
    required this.email,
    this.name,
    this.avatarUrl,
    this.educationStage,
    this.enrollmentYear,
    this.role,
  });

  factory UserProfileDto.fromJson(Map<String, Object?> json) {
    return UserProfileDto(
      id: json['id']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      name: json['name']?.toString(),
      avatarUrl: json['avatarUrl']?.toString(),
      educationStage: json['educationStage']?.toString(),
      enrollmentYear: nullableIntValue(json['enrollmentYear']),
      role: json['role']?.toString(),
    );
  }

  final String id;
  final String email;
  final String? name;
  final String? avatarUrl;
  final String? educationStage;
  final int? enrollmentYear;
  final String? role;
}

class AuthDtoMapper {
  const AuthDtoMapper();

  Map<String, Object?> loginRequest({
    required String email,
    required String password,
  }) {
    return {'email': email, 'password': password};
  }

  Map<String, Object?> registerRequest({
    required String email,
    required String password,
    String? name,
  }) {
    return {
      'email': email,
      'password': password,
      if (name != null && name.trim().isNotEmpty) 'name': name.trim(),
    };
  }

  Map<String, Object?> updateProfileRequest({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  }) {
    return {
      'name': name,
      'educationStage': educationStage,
      'enrollmentYear': enrollmentYear,
    };
  }

  Map<String, Object?> logoutRequest(String refreshToken) {
    return {'refreshToken': refreshToken};
  }

  Map<String, Object?> refreshRequest(String refreshToken) {
    return {'refreshToken': refreshToken};
  }

  AuthSession sessionFromJson(Map<String, Object?> data) {
    final userPayload = data['user'];
    if (userPayload is! Map<String, Object?>) {
      throw const FormatException('登录响应缺少 user');
    }

    return AuthSession(
      user: _userToDomain(UserProfileDto.fromJson(userPayload)),
      accessToken: data['accessToken']?.toString() ?? '',
      refreshToken: data['refreshToken']?.toString() ?? '',
    );
  }

  UserProfile userFromJson(Map<String, Object?> data) {
    return _userToDomain(UserProfileDto.fromJson(data));
  }

  TokenPair refreshTokensFromJson(
    Map<String, Object?> data, {
    required String previousRefreshToken,
  }) {
    return TokenPair(
      accessToken: data['accessToken']?.toString() ?? '',
      refreshToken: data['refreshToken']?.toString() ?? previousRefreshToken,
    );
  }

  UserProfile _userToDomain(UserProfileDto dto) {
    return UserProfile(
      id: dto.id,
      email: dto.email,
      name: dto.name,
      avatarUrl: dto.avatarUrl,
      educationStage: dto.educationStage,
      enrollmentYear: dto.enrollmentYear,
      role: dto.role,
    );
  }
}
