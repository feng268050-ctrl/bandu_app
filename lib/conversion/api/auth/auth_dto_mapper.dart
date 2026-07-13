import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class LoginRequestDto {
  const LoginRequestDto({required this.email, required this.password});

  final String email;
  final String password;

  JsonObject toJson() => {'email': email, 'password': password};
}

class RegisterRequestDto {
  const RegisterRequestDto({
    required this.email,
    required this.password,
    this.name,
  });

  final String email;
  final String password;
  final String? name;

  JsonObject toJson() {
    final normalizedName = name?.trim();
    return {
      'email': email,
      'password': password,
      if (normalizedName != null && normalizedName.isNotEmpty)
        'name': normalizedName,
    };
  }
}

class UpdateProfileRequestDto {
  const UpdateProfileRequestDto({
    required this.name,
    required this.educationStage,
    required this.enrollmentYear,
  });

  final String name;
  final String educationStage;
  final int enrollmentYear;

  JsonObject toJson() => {
        'name': name,
        'educationStage': educationStage,
        'enrollmentYear': enrollmentYear,
      };
}

class LogoutRequestDto {
  const LogoutRequestDto({required this.refreshToken});

  final String refreshToken;

  JsonObject toJson() => {'refreshToken': refreshToken};
}

class RefreshSessionRequestDto {
  const RefreshSessionRequestDto({required this.refreshToken});

  final String refreshToken;

  JsonObject toJson() => {'refreshToken': refreshToken};
}

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

  factory UserProfileDto.fromJson(JsonObject json) {
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

class AuthSessionDto {
  const AuthSessionDto({
    required this.user,
    required this.accessToken,
    required this.refreshToken,
  });

  factory AuthSessionDto.fromJson(JsonObject json) {
    return AuthSessionDto(
      user: UserProfileDto.fromJson(
        requireJsonObject(json['user'], context: 'AuthSessionDto.user'),
      ),
      accessToken: json['accessToken']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString() ?? '',
    );
  }

  final UserProfileDto user;
  final String accessToken;
  final String refreshToken;
}

class RefreshSessionDto {
  const RefreshSessionDto({required this.accessToken, this.refreshToken});

  factory RefreshSessionDto.fromJson(JsonObject json) {
    return RefreshSessionDto(
      accessToken: json['accessToken']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString(),
    );
  }

  final String accessToken;
  final String? refreshToken;
}

class AuthDtoMapper {
  const AuthDtoMapper();

  LoginRequestDto loginRequest({
    required String email,
    required String password,
  }) {
    return LoginRequestDto(email: email, password: password);
  }

  RegisterRequestDto registerRequest({
    required String email,
    required String password,
    String? name,
  }) {
    return RegisterRequestDto(email: email, password: password, name: name);
  }

  UpdateProfileRequestDto updateProfileRequest({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  }) {
    return UpdateProfileRequestDto(
      name: name,
      educationStage: educationStage,
      enrollmentYear: enrollmentYear,
    );
  }

  LogoutRequestDto logoutRequest(String refreshToken) {
    return LogoutRequestDto(refreshToken: refreshToken);
  }

  AuthSession sessionFromDto(AuthSessionDto dto) {
    return AuthSession(
      user: userFromDto(dto.user),
      accessToken: dto.accessToken,
      refreshToken: dto.refreshToken,
    );
  }

  UserProfile userFromDto(UserProfileDto dto) {
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

  TokenPair refreshTokensFromDto(
    RefreshSessionDto dto, {
    required String previousRefreshToken,
  }) {
    return TokenPair(
      accessToken: dto.accessToken,
      refreshToken: dto.refreshToken ?? previousRefreshToken,
    );
  }
}
