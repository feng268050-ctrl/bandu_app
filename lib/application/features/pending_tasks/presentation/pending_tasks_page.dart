import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/pending_task_providers.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/presentation/pending_tasks_controller.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class PendingTasksPage extends ConsumerWidget {
  const PendingTasksPage({required this.onBack, super.key});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tasks = ref.watch(pendingTasksProvider);
    final actionState = ref.watch(pendingTasksControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('待上传任务'),
        leading: BackButton(onPressed: onBack),
        actions: [
          IconButton(
            tooltip: '全部重试',
            onPressed:
                tasks.valueOrNull?.any((task) => task.canRetry) == true &&
                        !actionState.isRetryingAll
                    ? () => _retryAll(context, ref)
                    : null,
            icon: actionState.isRetryingAll
                ? const SizedBox.square(
                    dimension: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.sync),
          ),
        ],
      ),
      body: tasks.when(
        loading: () => const AppLoadingView(message: '正在读取待上传任务'),
        error: (error, stackTrace) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: () => ref.invalidate(pendingTasksProvider),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const AppEmptyView(
              icon: Icons.cloud_done_outlined,
              title: '没有待上传任务',
              message: '离线保存的错题会显示在这里，并在网络恢复后自动重试。',
            );
          }

          return ListView.separated(
            padding: const EdgeInsets.all(AppSpacing.page),
            itemCount: items.length,
            separatorBuilder: (context, index) => const Divider(),
            itemBuilder: (context, index) {
              final task = items[index];
              final isRunning = task.status == PendingTaskStatus.running ||
                  actionState.isRunning(task.id);
              return _PendingTaskTile(
                task: task,
                isRunning: isRunning,
                onRetry: task.canRetry && !isRunning
                    ? () => _retryOne(context, ref, task.id)
                    : null,
                onDelete: isRunning ? null : () => _delete(context, ref, task),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _retryOne(
    BuildContext context,
    WidgetRef ref,
    String taskId,
  ) async {
    final result =
        await ref.read(pendingTasksControllerProvider.notifier).retry(taskId);
    if (!context.mounted) {
      return;
    }
    if (result.isSuccess) {
      _refreshRemoteData(ref);
      showAppSuccessSnackBar(context, result.message);
    } else {
      showAppErrorSnackBar(context, result.message);
    }
  }

  Future<void> _retryAll(BuildContext context, WidgetRef ref) async {
    final summary =
        await ref.read(pendingTasksControllerProvider.notifier).retryAll();
    if (!context.mounted) {
      return;
    }
    if (summary.succeeded > 0) {
      _refreshRemoteData(ref);
    }
    final message = summary.attempted == 0
        ? '没有可重试的任务'
        : '已上传 ${summary.succeeded} 个，失败 ${summary.failed} 个';
    if (summary.failed == 0) {
      showAppSuccessSnackBar(context, message);
    } else {
      showAppErrorSnackBar(context, message);
    }
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    PendingTask task,
  ) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除待上传任务',
      message: '将删除“${task.result.title}”及其本地临时图片，且无法恢复。',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed) {
      return;
    }
    try {
      await ref.read(pendingTasksControllerProvider.notifier).delete(task.id);
      if (context.mounted) {
        showAppSuccessSnackBar(context, '待上传任务已删除');
      }
    } catch (error) {
      if (context.mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
  }

  void _refreshRemoteData(WidgetRef ref) {
    ref.invalidate(libraryControllerProvider);
    ref.invalidate(statsOverviewProvider);
  }
}

class _PendingTaskTile extends StatelessWidget {
  const _PendingTaskTile({
    required this.task,
    required this.isRunning,
    required this.onRetry,
    required this.onDelete,
  });

  final PendingTask task;
  final bool isRunning;
  final VoidCallback? onRetry;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: isRunning
          ? const SizedBox.square(
              dimension: 24,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Icon(_statusIcon(task.status)),
      title:
          Text(task.result.title, maxLines: 2, overflow: TextOverflow.ellipsis),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: AppSpacing.xSmall),
          Text('${_statusLabel(task)} · ${_formatTime(task.createdAt)}'),
          if (task.lastError != null) ...[
            const SizedBox(height: AppSpacing.xSmall),
            Text(
              task.lastError!,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        ],
      ),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            tooltip: '重试',
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: '删除',
            onPressed: onDelete,
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
    );
  }
}

String _statusLabel(PendingTask task) {
  return switch (task.status) {
    PendingTaskStatus.pending => '等待上传',
    PendingTaskStatus.running => '正在上传',
    PendingTaskStatus.failed when !task.canRetry => '已停止重试',
    PendingTaskStatus.failed =>
      '上传失败（${task.retryCount}/$maxPendingTaskRetries）',
    PendingTaskStatus.completed => '正在清理',
  };
}

IconData _statusIcon(PendingTaskStatus status) {
  return switch (status) {
    PendingTaskStatus.pending => Icons.cloud_upload_outlined,
    PendingTaskStatus.running => Icons.sync,
    PendingTaskStatus.failed => Icons.cloud_off_outlined,
    PendingTaskStatus.completed => Icons.cloud_done_outlined,
  };
}

String _formatTime(DateTime value) {
  final local = value.toLocal();
  String twoDigits(int number) => number.toString().padLeft(2, '0');
  return '${local.year}-${twoDigits(local.month)}-${twoDigits(local.day)} '
      '${twoDigits(local.hour)}:${twoDigits(local.minute)}';
}
