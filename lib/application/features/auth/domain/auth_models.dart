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

  final String id;
  final String email;
  final String? name;
  final String? avatarUrl;
  final String? educationStage;
  final int? enrollmentYear;
  final String? role;
}

const guestUserProfile = UserProfile(
  id: 'guest-local',
  email: 'guest@local.dev',
  name: '本地体验用户',
  educationStage: '初中',
  enrollmentYear: 2024,
);
