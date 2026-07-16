import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/presentation/network_diagnostics_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class NetworkDiagnosticsPage extends ConsumerWidget {
  const NetworkDiagnosticsPage({required this.onBack, super.key});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final diagnostics = ref.watch(networkDiagnosticsControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('网络诊断'),
        leading: BackButton(onPressed: onBack),
      ),
      body: diagnostics.when(
        loading: () => const AppLoadingView(message: '正在读取网络配置'),
        error: (error, stackTrace) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: () => ref.invalidate(networkDiagnosticsControllerProvider),
        ),
        data: (state) {
          final snapshot = state.snapshot;
          final health = state.healthCheck;
          return ListView(
            padding: const EdgeInsets.all(AppSpacing.page),
            children: [
              _DiagnosticRow(label: 'API Host', value: snapshot.apiHost),
              _DiagnosticRow(
                label: 'API Base Path',
                value: snapshot.apiBasePath,
              ),
              _DiagnosticRow(
                label: 'HTTPS 状态',
                value: snapshot.isHttps ? '已启用' : '未启用（仅限 Debug）',
              ),
              _DiagnosticRow(
                label: '健康检查地址',
                value: snapshot.healthCheckUrl,
              ),
              _DiagnosticRow(
                label: '健康检查结果',
                value: health?.message ?? '尚未检查',
              ),
              _DiagnosticRow(
                label: '最近一次请求',
                value: _formatTime(snapshot.lastRequestAt),
              ),
              _DiagnosticRow(label: 'App 版本', value: snapshot.appVersion),
              _DiagnosticRow(label: '构建模式', value: snapshot.buildMode),
              _DiagnosticRow(label: '运行环境', value: snapshot.environment),
              const SizedBox(height: AppSpacing.large),
              AppDefaultButton(
                label: state.isChecking ? '正在检查' : '检查服务',
                expanded: true,
                onPressed: state.isChecking
                    ? null
                    : ref
                        .read(networkDiagnosticsControllerProvider.notifier)
                        .checkHealth,
                icon: state.isChecking
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.health_and_safety_outlined),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _DiagnosticRow extends StatelessWidget {
  const _DiagnosticRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(label),
      subtitle: Text(value),
    );
  }
}

String _formatTime(DateTime? value) {
  if (value == null) {
    return '暂无请求';
  }
  final local = value.toLocal();
  String twoDigits(int number) => number.toString().padLeft(2, '0');
  return '${local.year}-${twoDigits(local.month)}-${twoDigits(local.day)} '
      '${twoDigits(local.hour)}:${twoDigits(local.minute)}:${twoDigits(local.second)}';
}
