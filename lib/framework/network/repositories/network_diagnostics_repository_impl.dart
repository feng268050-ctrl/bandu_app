import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_models.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/common/dio_failure_mapper.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/network/client/network_activity_tracker.dart';
import 'package:bandu_wrong_notebook/framework/network/services/health_api_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final frameworkNetworkDiagnosticsRepositoryProvider =
    Provider<NetworkDiagnosticsRepository>((ref) {
  return FrameworkNetworkDiagnosticsRepository(
    config: ref.watch(appConfigProvider),
    healthApiService: ref.watch(healthApiServiceProvider),
    activityTracker: ref.watch(networkActivityTrackerProvider),
  );
});

class FrameworkNetworkDiagnosticsRepository
    implements NetworkDiagnosticsRepository {
  const FrameworkNetworkDiagnosticsRepository({
    required this.config,
    required this.healthApiService,
    required this.activityTracker,
    this.failureMapper = const DioFailureMapper(),
  });

  final AppConfig config;
  final HealthApiService healthApiService;
  final NetworkActivityTracker activityTracker;
  final DioFailureMapper failureMapper;

  @override
  Future<NetworkDiagnosticsSnapshot> readSnapshot() async {
    final uri = config.apiBaseUri;
    return NetworkDiagnosticsSnapshot(
      apiHost: uri.host,
      apiBasePath: uri.path,
      healthCheckUrl: config.healthCheckUri.toString(),
      isHttps: uri.scheme == 'https',
      lastRequestAt: activityTracker.lastRequestAt,
      appVersion: config.appVersion,
      buildMode: config.buildModeLabel,
      environment: config.environment.value,
    );
  }

  @override
  Future<HealthCheckResult> checkHealth() async {
    final stopwatch = Stopwatch()..start();
    try {
      final statusCode = await healthApiService.check();
      stopwatch.stop();
      final isHealthy =
          statusCode != null && statusCode >= 200 && statusCode < 300;
      return HealthCheckResult(
        isHealthy: isHealthy,
        message: isHealthy ? '服务连接正常' : '服务响应异常，请稍后重试。',
        checkedAt: DateTime.now(),
        elapsed: stopwatch.elapsed,
        statusCode: statusCode,
      );
    } on DioException catch (error) {
      stopwatch.stop();
      final failure = failureMapper.map(error);
      return HealthCheckResult(
        isHealthy: false,
        message: failure.userMessage,
        checkedAt: DateTime.now(),
        elapsed: stopwatch.elapsed,
        statusCode: failure.statusCode,
      );
    } catch (error) {
      stopwatch.stop();
      return HealthCheckResult(
        isHealthy: false,
        message: appFailureUserMessage(error),
        checkedAt: DateTime.now(),
        elapsed: stopwatch.elapsed,
      );
    }
  }
}
