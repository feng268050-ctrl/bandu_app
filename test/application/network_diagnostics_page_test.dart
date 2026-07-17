import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_models.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/domain/network_diagnostics_repository.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/network_diagnostics_providers.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/presentation/network_diagnostics_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('network diagnostics shows public config and health result',
      (tester) async {
    final repository = _FakeNetworkDiagnosticsRepository();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          networkDiagnosticsRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          home: NetworkDiagnosticsPage(onBack: () {}),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('aibandu.dpdns.org'), findsOneWidget);
    expect(find.text('/api/mobile/v1'), findsOneWidget);
    expect(find.text('已启用'), findsOneWidget);
    expect(find.text('尚未检查'), findsOneWidget);
    expect(find.textContaining('Token'), findsNothing);

    await tester.drag(find.byType(ListView), const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('检查服务'));
    await tester.pumpAndSettle();

    expect(find.text('服务连接正常'), findsOneWidget);
    expect(repository.healthCalls, 1);
  });
}

class _FakeNetworkDiagnosticsRepository
    implements NetworkDiagnosticsRepository {
  int healthCalls = 0;

  @override
  Future<HealthCheckResult> checkHealth() async {
    healthCalls += 1;
    return HealthCheckResult(
      isHealthy: true,
      message: '服务连接正常',
      checkedAt: DateTime.utc(2026, 7, 16),
      elapsed: const Duration(milliseconds: 80),
      statusCode: 200,
    );
  }

  @override
  Future<NetworkDiagnosticsSnapshot> readSnapshot() async {
    return NetworkDiagnosticsSnapshot(
      apiHost: 'aibandu.dpdns.org',
      apiBasePath: '/api/mobile/v1',
      healthCheckUrl: 'https://aibandu.dpdns.org/api/health',
      isHttps: true,
      lastRequestAt: DateTime.utc(2026, 7, 16),
      appVersion: 'v0.2.0',
      buildMode: 'Release',
      environment: 'production',
    );
  }
}
