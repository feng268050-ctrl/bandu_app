import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';

const maxPendingTaskRetries = 5;

enum PendingTaskKind { saveAnalyzedCapture }

enum PendingTaskStatus { pending, running, failed, completed }

class PendingTask {
  const PendingTask({
    required this.id,
    required this.kind,
    required this.localImagePath,
    required this.result,
    required this.createdAt,
    required this.updatedAt,
    this.status = PendingTaskStatus.pending,
    this.retryCount = 0,
    this.lastError,
  });

  final String id;
  final PendingTaskKind kind;
  final String localImagePath;
  final AnalyzeResult result;
  final DateTime createdAt;
  final DateTime updatedAt;
  final PendingTaskStatus status;
  final int retryCount;
  final String? lastError;

  bool get canRetry =>
      status != PendingTaskStatus.running &&
      status != PendingTaskStatus.completed &&
      retryCount < maxPendingTaskRetries;

  PendingTask markRunning(DateTime now) {
    return _copyWith(
      status: PendingTaskStatus.running,
      updatedAt: now,
      clearLastError: true,
    );
  }

  PendingTask markPending(DateTime now, {String? message}) {
    return _copyWith(
      status: PendingTaskStatus.pending,
      updatedAt: now,
      lastError: message,
      clearLastError: message == null,
    );
  }

  PendingTask markFailed(
    DateTime now,
    String message, {
    bool permanent = false,
  }) {
    return _copyWith(
      status: PendingTaskStatus.failed,
      updatedAt: now,
      retryCount: permanent ? maxPendingTaskRetries : retryCount + 1,
      lastError: message,
    );
  }

  PendingTask markCompleted(DateTime now) {
    return _copyWith(
      status: PendingTaskStatus.completed,
      updatedAt: now,
      clearLastError: true,
    );
  }

  PendingTask _copyWith({
    required PendingTaskStatus status,
    required DateTime updatedAt,
    int? retryCount,
    String? lastError,
    bool clearLastError = false,
  }) {
    return PendingTask(
      id: id,
      kind: kind,
      localImagePath: localImagePath,
      result: result,
      createdAt: createdAt,
      updatedAt: updatedAt,
      status: status,
      retryCount: retryCount ?? this.retryCount,
      lastError: clearLastError ? null : lastError ?? this.lastError,
    );
  }
}

enum PendingTaskRunOutcome { succeeded, failed, cancelled, skipped }

class PendingTaskRunResult {
  const PendingTaskRunResult({
    required this.taskId,
    required this.outcome,
    required this.message,
    this.savedErrorItemId,
  });

  final String taskId;
  final PendingTaskRunOutcome outcome;
  final String message;
  final String? savedErrorItemId;

  bool get isSuccess => outcome == PendingTaskRunOutcome.succeeded;
}

class PendingTaskRetrySummary {
  const PendingTaskRetrySummary({
    required this.attempted,
    required this.succeeded,
    required this.failed,
    required this.cancelled,
    required this.skipped,
  });

  const PendingTaskRetrySummary.empty()
      : attempted = 0,
        succeeded = 0,
        failed = 0,
        cancelled = 0,
        skipped = 0;

  final int attempted;
  final int succeeded;
  final int failed;
  final int cancelled;
  final int skipped;
}
