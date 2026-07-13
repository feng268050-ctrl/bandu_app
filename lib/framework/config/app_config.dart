import 'package:flutter_riverpod/flutter_riverpod.dart';

final appConfigProvider = Provider<AppConfig>((ref) {
  return const AppConfig(
    apiBaseUrl: String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://10.0.2.2:3000/api/mobile/v1',
    ),
  );
});

class AppConfig {
  const AppConfig({required this.apiBaseUrl});

  final String apiBaseUrl;
}
