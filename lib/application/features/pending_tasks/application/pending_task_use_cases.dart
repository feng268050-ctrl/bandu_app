import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_repository.dart';

class WatchPendingTasksUseCase {
  const WatchPendingTasksUseCase(this._repository);

  final PendingTaskRepository _repository;

  Stream<List<PendingTask>> call() => _repository.watchTasks();
}

class QueueAnalyzedCaptureUseCase {
  const QueueAnalyzedCaptureUseCase(this._repository);

  final PendingTaskRepository _repository;

  Future<PendingTask> call({
    required String localImagePath,
    required AnalyzeResult result,
  }) {
    return _repository.enqueueAnalyzedCapture(
      localImagePath: localImagePath,
      result: result,
    );
  }
}

class RetryPendingTaskUseCase {
  const RetryPendingTaskUseCase(this._repository);

  final PendingTaskRepository _repository;

  Future<PendingTaskRunResult> call(String taskId) {
    return _repository.retryTask(taskId);
  }
}

class RetryAllPendingTasksUseCase {
  const RetryAllPendingTasksUseCase(this._repository);

  final PendingTaskRepository _repository;

  Future<PendingTaskRetrySummary> call() => _repository.retryAll();
}

class CancelPendingTaskUseCase {
  const CancelPendingTaskUseCase(this._repository);

  final PendingTaskRepository _repository;

  void call(String taskId) => _repository.cancelTask(taskId);
}

class DeletePendingTaskUseCase {
  const DeletePendingTaskUseCase(this._repository);

  final PendingTaskRepository _repository;

  Future<void> call(String taskId) => _repository.deleteTask(taskId);
}
