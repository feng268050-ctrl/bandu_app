import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_models.dart';

abstract interface class NetworkDiagnosticsRepository {
  Future<NetworkDiagnosticsSnapshot> readSnapshot();

  Future<HealthCheckResult> checkHealth();
}
