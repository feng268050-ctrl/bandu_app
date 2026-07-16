enum CapturePhase {
  idle,
  capturing,
  preview,
  uploading,
  analyzing,
  success,
  failed,
}

class CaptureUiState {
  const CaptureUiState({
    required this.phase,
    this.localImagePath,
    this.result,
    this.savedErrorItemId,
    this.queuedTaskId,
    this.errorMessage,
    this.noticeMessage,
  });

  factory CaptureUiState.initial() {
    return const CaptureUiState(phase: CapturePhase.idle);
  }

  final CapturePhase phase;
  final String? localImagePath;
  final AnalyzeResult? result;
  final String? savedErrorItemId;
  final String? queuedTaskId;
  final String? errorMessage;
  final String? noticeMessage;

  bool get canAnalyze =>
      localImagePath != null &&
      result == null &&
      (phase == CapturePhase.preview || phase == CapturePhase.failed);

  bool get canSave =>
      localImagePath != null &&
      result != null &&
      savedErrorItemId == null &&
      queuedTaskId == null &&
      (phase == CapturePhase.success || phase == CapturePhase.failed);

  CaptureUiState copyWith({
    CapturePhase? phase,
    String? localImagePath,
    AnalyzeResult? result,
    String? savedErrorItemId,
    String? queuedTaskId,
    String? errorMessage,
    String? noticeMessage,
  }) {
    return CaptureUiState(
      phase: phase ?? this.phase,
      localImagePath: localImagePath ?? this.localImagePath,
      result: result ?? this.result,
      savedErrorItemId: savedErrorItemId ?? this.savedErrorItemId,
      queuedTaskId: queuedTaskId ?? this.queuedTaskId,
      errorMessage: errorMessage,
      noticeMessage: noticeMessage,
    );
  }
}

class AnalyzeResult {
  const AnalyzeResult({
    required this.title,
    this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.tags = const [],
  });

  final String title;
  final String? subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final List<String> tags;
}

class SavedErrorItem {
  const SavedErrorItem({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.updatedAt,
  });

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final DateTime? updatedAt;
}
