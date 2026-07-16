import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_models.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/network_diagnostics_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final networkDiagnosticsControllerProvider = AsyncNotifierProvider<
    NetworkDiagnosticsController, NetworkDiagnosticsState>(
  NetworkDiagnosticsController.new,
);

class NetworkDiagnosticsController
    extends AsyncNotifier<NetworkDiagnosticsState> {
  @override
  Future<NetworkDiagnosticsState> build() async {
    final snapshot =
        await ref.read(readNetworkDiagnosticsUseCaseProvider).call();
    return NetworkDiagnosticsState(snapshot: snapshot);
  }

  Future<void> checkHealth() async {
    final current = state.valueOrNull;
    if (current == null || current.isChecking) {
      return;
    }

    state = AsyncData(current.copyWith(isChecking: true));
    final result = await ref.read(checkHealthUseCaseProvider).call();
    final snapshot =
        await ref.read(readNetworkDiagnosticsUseCaseProvider).call();
    state = AsyncData(
      NetworkDiagnosticsState(
        snapshot: snapshot,
        healthCheck: result,
      ),
    );
  }
}
