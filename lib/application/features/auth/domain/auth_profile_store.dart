import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';

abstract interface class AuthProfileStore {
  Future<UserProfile?> read();

  Future<void> save(UserProfile profile);

  Future<void> clear();
}
