enum PendingTaskKind {
  uploadCapture,
  saveErrorItem,
  updatePracticeResult,
}

enum PendingTaskStatus {
  pending,
  running,
  failed,
  completed,
}

class PendingTask {
  const PendingTask({
    required this.id,
    required this.kind,
    required this.payload,
    required this.createdAt,
    this.status = PendingTaskStatus.pending,
    this.retryCount = 0,
    this.lastError,
  });

  final String id;
  final PendingTaskKind kind;
  final Map<String, Object?> payload;
  final DateTime createdAt;
  final PendingTaskStatus status;
  final int retryCount;
  final String? lastError;

  PendingTask copyWith({
    PendingTaskStatus? status,
    int? retryCount,
    String? lastError,
  }) {
    return PendingTask(
      id: id,
      kind: kind,
      payload: payload,
      createdAt: createdAt,
      status: status ?? this.status,
      retryCount: retryCount ?? this.retryCount,
      lastError: lastError,
    );
  }
}
