import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('environment and endpoint resolution', () {
    test('uses Android emulator host for an unconfigured debug build', () {
      expect(
        resolveAppApiBaseUrl(
          configuredApiBaseUrl: '',
          releaseMode: false,
        ),
        emulatorApiBaseUrl,
      );
    });

    test('keeps an explicit development endpoint', () {
      const endpoint = 'http://100.69.41.14:3000/api/mobile/v1';
      expect(
        resolveAppApiBaseUrl(
          configuredApiBaseUrl: endpoint,
          releaseMode: false,
        ),
        endpoint,
      );
    });

    test('production defaults to the single public endpoint', () {
      expect(
        resolveAppApiBaseUrl(
          configuredApiBaseUrl: '',
          releaseMode: true,
        ),
        productionApiBaseUrl,
      );
    });

    test('release cannot select a non-production environment', () {
      expect(
        () => resolveAppEnvironment(
          configuredEnvironment: 'development',
          releaseMode: true,
        ),
        throwsStateError,
      );
    });
  });

  group('production validation', () {
    const valid = AppConfig(
      environment: AppEnvironment.production,
      apiBaseUrl: productionApiBaseUrl,
      releaseMode: true,
    );

    test('accepts the exact public HTTPS API and derives health URL', () {
      expect(valid.validate, returnsNormally);
      expect(valid.dioBaseUrl, '$productionApiBaseUrl/');
      expect(valid.healthCheckUri.toString(), productionHealthCheckUrl);
    });

    test('rejects local, insecure, subdomain, and bypass configurations', () {
      final invalid = [
        const AppConfig(
          environment: AppEnvironment.production,
          apiBaseUrl: emulatorApiBaseUrl,
          releaseMode: true,
        ),
        const AppConfig(
          environment: AppEnvironment.production,
          apiBaseUrl: 'http://aibandu.dpdns.org/api/mobile/v1',
          releaseMode: true,
        ),
        const AppConfig(
          environment: AppEnvironment.production,
          apiBaseUrl: 'https://api.aibandu.dpdns.org/api/mobile/v1',
          releaseMode: true,
        ),
        const AppConfig(
          environment: AppEnvironment.production,
          apiBaseUrl: productionApiBaseUrl,
          releaseMode: true,
          bypassAuth: true,
        ),
      ];

      for (final config in invalid) {
        expect(config.validate, throwsStateError);
      }
    });

    test('rejects duplicated or missing mobile API path', () {
      for (final endpoint in [
        'https://aibandu.dpdns.org/api/mobile/v1/api/mobile/v1',
        'https://aibandu.dpdns.org',
      ]) {
        final config = AppConfig(
          environment: AppEnvironment.production,
          apiBaseUrl: endpoint,
          releaseMode: true,
        );
        expect(config.validate, throwsStateError);
      }
    });
  });
}
