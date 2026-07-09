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
    this.errorMessage,
  });

  factory CaptureUiState.initial() {
    return const CaptureUiState(phase: CapturePhase.idle);
  }

  final CapturePhase phase;
  final String? localImagePath;
  final AnalyzeResult? result;
  final String? errorMessage;

  bool get canAnalyze =>
      phase == CapturePhase.preview && localImagePath != null;

  CaptureUiState copyWith({
    CapturePhase? phase,
    String? localImagePath,
    AnalyzeResult? result,
    String? errorMessage,
  }) {
    return CaptureUiState(
      phase: phase ?? this.phase,
      localImagePath: localImagePath ?? this.localImagePath,
      result: result ?? this.result,
      errorMessage: errorMessage,
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

  factory AnalyzeResult.fromJson(Map<String, Object?> json) {
    return AnalyzeResult(
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString(),
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      tags: switch (json['tags']) {
        final List<Object?> tags => tags.map((tag) => tag.toString()).toList(),
        _ => const [],
      },
    );
  }

  final String title;
  final String? subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final List<String> tags;
}
