import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';

abstract interface class AiConfigRepository {
  Future<List<AiServiceConfig>> fetchConfigs();

  Future<AiServiceConfig> createConfig(AiServiceConfigDraft draft);

  Future<AiServiceConfig> updateConfig(
    String id,
    AiServiceConfigDraft draft,
  );

  Future<AiServiceConfig> setDefault(String id);

  Future<void> deleteConfig(String id);
}
