import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

const emulatorApiBaseUrl = 'http://10.0.2.2:3000/api/mobile/v1';
const productionApiBaseUrl = 'https://aibandu.dpdns.org/api/mobile/v1';
const productionHealthCheckUrl = 'https://aibandu.dpdns.org/api/health';

// Kept as an alias for callers that still use the previous constant name.
const releaseApiBaseUrl = productionApiBaseUrl;

enum AppEnvironment { development, staging, production }

extension AppEnvironmentName on AppEnvironment {
  String get value => switch (this) {
        AppEnvironment.development => 'development',
        AppEnvironment.staging => 'staging',
        AppEnvironment.production => 'production',
      };
}

final appConfigProvider = Provider<AppConfig>((ref) => loadAppConfig());

AppConfig loadAppConfig({bool releaseMode = kReleaseMode}) {
  const configuredEnvironment = String.fromEnvironment('APP_ENV');
  const configuredApiBaseUrl = String.fromEnvironment('API_BASE_URL');
  const bypassAuth = bool.fromEnvironment('BYPASS_AUTH');
  const appVersion = String.fromEnvironment(
    'APP_VERSION',
    defaultValue: 'v0.2.0',
  );

  final environment = resolveAppEnvironment(
    configuredEnvironment: configuredEnvironment,
    releaseMode: releaseMode,
  );
  final config = AppConfig(
    environment: environment,
    apiBaseUrl: resolveAppApiBaseUrl(
      configuredApiBaseUrl: configuredApiBaseUrl,
      releaseMode: releaseMode,
      environment: environment,
    ),
    bypassAuth: bypassAuth,
    appVersion: appVersion,
    releaseMode: releaseMode,
  );
  config.validate();
  return config;
}

AppEnvironment resolveAppEnvironment({
  required String configuredEnvironment,
  required bool releaseMode,
}) {
  final configured = configuredEnvironment.trim().toLowerCase();
  final environment = switch (configured) {
    '' => releaseMode ? AppEnvironment.production : AppEnvironment.development,
    'dev' || 'development' => AppEnvironment.development,
    'stage' || 'staging' => AppEnvironment.staging,
    'prod' || 'production' => AppEnvironment.production,
    _ => throw StateError('Unknown APP_ENV: $configuredEnvironment'),
  };

  if (releaseMode && environment != AppEnvironment.production) {
    throw StateError('Release builds require APP_ENV=production');
  }
  return environment;
}

String resolveAppApiBaseUrl({
  required String configuredApiBaseUrl,
  required bool releaseMode,
  AppEnvironment? environment,
}) {
  final resolvedEnvironment = environment ??
      (releaseMode ? AppEnvironment.production : AppEnvironment.development);
  final configured = _withoutTrailingSlash(configuredApiBaseUrl.trim());

  if (configured.isNotEmpty) {
    return configured;
  }
  if (resolvedEnvironment == AppEnvironment.production || releaseMode) {
    return productionApiBaseUrl;
  }
  if (resolvedEnvironment == AppEnvironment.staging) {
    throw StateError('Staging requires an explicit API_BASE_URL');
  }
  return emulatorApiBaseUrl;
}

class AppConfig {
  const AppConfig({
    required this.environment,
    required this.apiBaseUrl,
    required this.releaseMode,
    this.bypassAuth = false,
    this.appVersion = 'v0.2.0',
  });

  final AppEnvironment environment;
  final String apiBaseUrl;
  final bool bypassAuth;
  final String appVersion;
  final bool releaseMode;

  Uri get apiBaseUri => Uri.parse(apiBaseUrl);

  String get dioBaseUrl => '${_withoutTrailingSlash(apiBaseUrl)}/';

  Uri get healthCheckUri => apiBaseUri.replace(
        path: '/api/health',
        query: null,
        fragment: null,
      );

  String get buildModeLabel => releaseMode ? 'Release' : 'Debug';

  void validate() {
    final uri = Uri.tryParse(apiBaseUrl);
    if (uri == null || !uri.hasScheme || uri.host.isEmpty) {
      throw StateError('API_BASE_URL must be an absolute URL');
    }
    if (uri.userInfo.isNotEmpty || uri.hasQuery || uri.hasFragment) {
      throw StateError(
          'API_BASE_URL must not include credentials or query data');
    }

    final normalizedPath = _withoutTrailingSlash(uri.path);
    if (normalizedPath != '/api/mobile/v1') {
      throw StateError('API_BASE_URL path must be /api/mobile/v1');
    }

    if (environment == AppEnvironment.production || releaseMode) {
      if (uri.scheme != 'https') {
        throw StateError('Production API must use HTTPS');
      }
      if (uri.host != 'aibandu.dpdns.org') {
        throw StateError('Production API host must be aibandu.dpdns.org');
      }
      if (uri.hasPort && uri.port != 443) {
        throw StateError('Production API must use the default HTTPS port');
      }
      if (_withoutTrailingSlash(apiBaseUrl) != productionApiBaseUrl) {
        throw StateError('Unexpected production API URL');
      }
      if (bypassAuth) {
        throw StateError('BYPASS_AUTH must be false in production');
      }
    } else if (environment == AppEnvironment.staging && uri.scheme != 'https') {
      throw StateError('Staging API must use HTTPS');
    }
  }
}

String _withoutTrailingSlash(String value) {
  var result = value;
  while (result.endsWith('/')) {
    result = result.substring(0, result.length - 1);
  }
  return result;
}
