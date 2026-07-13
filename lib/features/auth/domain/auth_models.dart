class AuthSession {
  const AuthSession({
    required this.user,
    required this.accessToken,
    required this.refreshToken,
  });

  final UserProfile user;
  final String accessToken;
  final String refreshToken;
}

class UserProfile {
  const UserProfile({
    required this.id,
    required this.email,
    this.name,
    this.avatarUrl,
    this.educationStage,
    this.enrollmentYear,
    this.role,
  });

  factory UserProfile.fromJson(Map<String, Object?> json) {
    return UserProfile(
      id: json['id']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      name: json['name']?.toString(),
      avatarUrl: json['avatarUrl']?.toString(),
      educationStage: json['educationStage']?.toString(),
      enrollmentYear: switch (json['enrollmentYear']) {
        final int value => value,
        final num value => value.toInt(),
        final String value => int.tryParse(value),
        _ => null,
      },
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
