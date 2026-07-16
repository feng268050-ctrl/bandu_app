import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_models.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_repository.dart';

class ReadNetworkDiagnosticsUseCase {
  const ReadNetworkDiagnosticsUseCase(this._repository);

  final NetworkDiagnosticsRepository _repository;

  Future<NetworkDiagnosticsSnapshot> call() {
    return _repository.readSnapshot();
  }
}

class CheckHealthUseCase {
  const CheckHealthUseCase(this._repository);

  final NetworkDiagnosticsRepository _repository;

  Future<HealthCheckResult> call() {
    return _repository.checkHealth();
  }
}
