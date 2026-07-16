import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/application/network_diagnostics_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final networkDiagnosticsRepositoryProvider =
    Provider<NetworkDiagnosticsRepository>(
  (ref) => missingDependency('NetworkDiagnosticsRepository'),
);

final readNetworkDiagnosticsUseCaseProvider =
    Provider<ReadNetworkDiagnosticsUseCase>((ref) {
  return ReadNetworkDiagnosticsUseCase(
    ref.watch(networkDiagnosticsRepositoryProvider),
  );
});

final checkHealthUseCaseProvider = Provider<CheckHealthUseCase>((ref) {
  return CheckHealthUseCase(ref.watch(networkDiagnosticsRepositoryProvider));
});
