class AiServiceConfig {
  const AiServiceConfig({
    required this.id,
    required this.name,
    required this.baseUrl,
    required this.model,
    required this.maskedApiKey,
    required this.hasApiKey,
    required this.isDefault,
  });

  final String id;
  final String name;
  final String baseUrl;
  final String model;
  final String maskedApiKey;
  final bool hasApiKey;
  final bool isDefault;
}

class AiServiceConfigDraft {
  const AiServiceConfigDraft({
    required this.name,
    required this.baseUrl,
    required this.apiKey,
    required this.model,
    required this.isDefault,
  });

  final String name;
  final String baseUrl;
  final String apiKey;
  final String model;
  final bool isDefault;
}
