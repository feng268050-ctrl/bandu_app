class HealthCheckResult {
  const HealthCheckResult({
    required this.isHealthy,
    required this.message,
    required this.checkedAt,
    required this.elapsed,
    this.statusCode,
  });

  final bool isHealthy;
  final String message;
  final DateTime checkedAt;
  final Duration elapsed;
  final int? statusCode;
}

class NetworkDiagnosticsSnapshot {
  const NetworkDiagnosticsSnapshot({
    required this.apiHost,
    required this.apiBasePath,
    required this.healthCheckUrl,
    required this.isHttps,
    required this.appVersion,
    required this.buildMode,
    required this.environment,
    this.lastRequestAt,
  });

  final String apiHost;
  final String apiBasePath;
  final String healthCheckUrl;
  final bool isHttps;
  final DateTime? lastRequestAt;
  final String appVersion;
  final String buildMode;
  final String environment;
}

class NetworkDiagnosticsState {
  const NetworkDiagnosticsState({
    required this.snapshot,
    this.healthCheck,
    this.isChecking = false,
  });

  final NetworkDiagnosticsSnapshot snapshot;
  final HealthCheckResult? healthCheck;
  final bool isChecking;

  NetworkDiagnosticsState copyWith({
    NetworkDiagnosticsSnapshot? snapshot,
    HealthCheckResult? healthCheck,
    bool? isChecking,
  }) {
    return NetworkDiagnosticsState(
      snapshot: snapshot ?? this.snapshot,
      healthCheck: healthCheck ?? this.healthCheck,
      isChecking: isChecking ?? this.isChecking,
    );
  }
}
