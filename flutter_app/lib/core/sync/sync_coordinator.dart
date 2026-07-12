import 'dart:async';

import 'package:bandu_wrong_notebook/core/sync/pending_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final syncCoordinatorProvider = Provider<SyncCoordinator>((ref) {
  final coordinator = SyncCoordinator();
  ref.onDispose(coordinator.dispose);
  return coordinator;
});

class SyncCoordinator {
  final List<PendingTask> _tasks = [];
  final StreamController<List<PendingTask>> _controller =
      StreamController<List<PendingTask>>.broadcast();

  Stream<List<PendingTask>> watchTasks() {
    Future<void>.microtask(_emit);
    return _controller.stream;
  }

  Future<void> enqueue(PendingTask task) async {
    _tasks.add(task);
    _emit();
  }

  Future<void> markCompleted(String taskId) async {
    _replace(
      taskId,
      (task) => task.copyWith(status: PendingTaskStatus.completed),
    );
  }

  Future<void> markFailed(String taskId, Object error) async {
    _replace(
      taskId,
      (task) => task.copyWith(
        status: PendingTaskStatus.failed,
        retryCount: task.retryCount + 1,
        lastError: error.toString(),
      ),
    );
  }

  void dispose() {
    _controller.close();
  }

  void _replace(String taskId, PendingTask Function(PendingTask task) update) {
    final index = _tasks.indexWhere((task) => task.id == taskId);
    if (index < 0) {
      return;
    }
    _tasks[index] = update(_tasks[index]);
    _emit();
  }

  void _emit() {
    if (!_controller.isClosed) {
      _controller.add(List.unmodifiable(_tasks));
    }
  }
}
