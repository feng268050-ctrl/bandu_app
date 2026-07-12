import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/features/auth/domain/auth_repository.dart';

class LoginUseCase {
  const LoginUseCase(this._repository);

  final AuthRepository _repository;

  Future<AuthSession> call({required String email, required String password}) {
    return _repository.login(email: email, password: password);
  }
}

class RestoreSessionUseCase {
  const RestoreSessionUseCase(this._repository);

  final AuthRepository _repository;

  Future<AuthSession?> call() {
    return _repository.restoreSession();
  }
}

class RegisterUseCase {
  const RegisterUseCase(this._repository);

  final AuthRepository _repository;

  Future<AuthSession> call({
    required String email,
    required String password,
    String? name,
  }) {
    return _repository.register(email: email, password: password, name: name);
  }
}

class LogoutUseCase {
  const LogoutUseCase(this._repository);

  final AuthRepository _repository;

  Future<void> call() {
    return _repository.logout();
  }
}

class UpdateProfileUseCase {
  const UpdateProfileUseCase(this._repository);

  final AuthRepository _repository;

  Future<UserProfile> call({
    required String name,
    required String educationStage,
    required int enrollmentYear,
  }) {
    return _repository.updateProfile(
      name: name,
      educationStage: educationStage,
      enrollmentYear: enrollmentYear,
    );
  }
}
