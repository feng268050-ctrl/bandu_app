import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/pending_task_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final pendingTasksControllerProvider =
    NotifierProvider<PendingTasksController, PendingTaskActionState>(
  PendingTasksController.new,
);

class PendingTaskActionState {
  const PendingTaskActionState({
    this.runningTaskIds = const {},
    this.isRetryingAll = false,
  });

  final Set<String> runningTaskIds;
  final bool isRetryingAll;

  bool isRunning(String taskId) => runningTaskIds.contains(taskId);
}

class PendingTasksController extends Notifier<PendingTaskActionState> {
  @override
  PendingTaskActionState build() => const PendingTaskActionState();

  Future<PendingTaskRunResult> retry(String taskId) async {
    state = PendingTaskActionState(
      runningTaskIds: {...state.runningTaskIds, taskId},
      isRetryingAll: state.isRetryingAll,
    );
    try {
      return await ref.read(retryPendingTaskUseCaseProvider).call(taskId);
    } finally {
      state = PendingTaskActionState(
        runningTaskIds: {...state.runningTaskIds}..remove(taskId),
        isRetryingAll: state.isRetryingAll,
      );
    }
  }

  Future<PendingTaskRetrySummary> retryAll() async {
    if (state.isRetryingAll) {
      return const PendingTaskRetrySummary.empty();
    }
    state = PendingTaskActionState(
      runningTaskIds: state.runningTaskIds,
      isRetryingAll: true,
    );
    try {
      return await ref.read(retryAllPendingTasksUseCaseProvider).call();
    } finally {
      state = PendingTaskActionState(
        runningTaskIds: state.runningTaskIds,
      );
    }
  }

  Future<void> delete(String taskId) {
    return ref.read(deletePendingTaskUseCaseProvider).call(taskId);
  }
}
