import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';

abstract interface class AuthRepository {
  Future<AuthSession> login({required String email, required String password});

  Future<AuthSession> register({
    required String email,
    required String password,
    String? name,
  });

  Future<AuthSession?> restoreSession();

  Future<UserProfile> updateProfile({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  });

  Future<void> logout();
}
