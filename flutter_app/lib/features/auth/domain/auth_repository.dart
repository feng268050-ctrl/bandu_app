import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';

abstract interface class AuthRepository {
  Future<AuthSession> login({
    required String email,
    required String password,
  });

  Future<AuthSession?> restoreSession();

  Future<void> logout();
}
