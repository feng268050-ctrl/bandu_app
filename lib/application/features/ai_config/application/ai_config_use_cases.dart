import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_repository.dart';

class LoadAiConfigsUseCase {
  const LoadAiConfigsUseCase(this._repository);

  final AiConfigRepository _repository;

  Future<List<AiServiceConfig>> call() => _repository.fetchConfigs();
}

class SaveAiConfigUseCase {
  const SaveAiConfigUseCase(this._repository);

  final AiConfigRepository _repository;

  Future<AiServiceConfig> call({
    required AiServiceConfigDraft draft,
    String? id,
  }) {
    return id == null
        ? _repository.createConfig(draft)
        : _repository.updateConfig(id, draft);
  }
}

class SetDefaultAiConfigUseCase {
  const SetDefaultAiConfigUseCase(this._repository);

  final AiConfigRepository _repository;

  Future<AiServiceConfig> call(String id) => _repository.setDefault(id);
}

class DeleteAiConfigUseCase {
  const DeleteAiConfigUseCase(this._repository);

  final AiConfigRepository _repository;

  Future<void> call(String id) => _repository.deleteConfig(id);
}
