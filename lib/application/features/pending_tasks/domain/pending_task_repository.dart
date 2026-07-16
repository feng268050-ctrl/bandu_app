import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';

abstract interface class PendingTaskRepository {
  Stream<List<PendingTask>> watchTasks();

  Future<List<PendingTask>> readTasks();

  Future<PendingTask> enqueueAnalyzedCapture({
    required String localImagePath,
    required AnalyzeResult result,
  });

  Future<PendingTaskRunResult> retryTask(String taskId);

  Future<PendingTaskRetrySummary> retryAll();

  void cancelTask(String taskId);

  Future<void> deleteTask(String taskId);

  Future<void> clear();
}
