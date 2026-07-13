import 'package:flutter_riverpod/flutter_riverpod.dart';

final appConfigProvider = Provider<AppConfig>((ref) {
  return const AppConfig(
    apiBaseUrl: String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://10.0.2.2:3000/api/mobile/v1',
    ),
    bypassAuth: bool.fromEnvironment(
      'BYPASS_AUTH',
      defaultValue: false,
    ),
  );
});

class AppConfig {
  const AppConfig({
    required this.apiBaseUrl,
    this.bypassAuth = false,
  });

  final String apiBaseUrl;

  /// 暂时跳过登录/注册页，直接进入主界面。
  final bool bypassAuth;
}
