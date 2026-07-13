import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';

class ClearLocalDataUseCase {
  const ClearLocalDataUseCase(this._repository);

  final LocalDataRepository _repository;

  Future<void> call() => _repository.clear();
}
