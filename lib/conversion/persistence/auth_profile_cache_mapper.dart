import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class CachedAuthProfile {
  const CachedAuthProfile({
    required this.id,
    required this.email,
    this.name,
    this.avatarUrl,
    this.educationStage,
    this.enrollmentYear,
    this.role,
  });

  factory CachedAuthProfile.fromJson(JsonObject json) {
    return CachedAuthProfile(
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

  JsonObject toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      'avatarUrl': avatarUrl,
      'educationStage': educationStage,
      'enrollmentYear': enrollmentYear,
      'role': role,
    };
  }
}

class AuthProfileCacheMapper {
  const AuthProfileCacheMapper();

  CachedAuthProfile toCache(UserProfile profile) {
    return CachedAuthProfile(
      id: profile.id,
      email: profile.email,
      name: profile.name,
      avatarUrl: profile.avatarUrl,
      educationStage: profile.educationStage,
      enrollmentYear: profile.enrollmentYear,
      role: profile.role,
    );
  }

  UserProfile? fromCache(CachedAuthProfile profile) {
    if (profile.id.isEmpty || profile.email.isEmpty) {
      return null;
    }
    return UserProfile(
      id: profile.id,
      email: profile.email,
      name: profile.name,
      avatarUrl: profile.avatarUrl,
      educationStage: profile.educationStage,
      enrollmentYear: profile.enrollmentYear,
      role: profile.role,
    );
  }
}
