class PracticeQuestion {
  const PracticeQuestion({
    required this.title,
    required this.questionText,
    required this.answer,
    required this.analysis,
    required this.subjectName,
    this.tags = const [],
  });

  factory PracticeQuestion.fromJson(Map<String, Object?> json) {
    return PracticeQuestion(
      title: json['title']?.toString() ?? '练习题',
      questionText: json['questionText']?.toString() ?? '',
      answer: json['answer']?.toString() ?? '',
      analysis: json['analysis']?.toString() ?? '',
      subjectName: json['subjectName']?.toString() ?? '其他',
      tags: switch (json['tags']) {
        final List<Object?> tags => tags.map((tag) => tag.toString()).toList(),
        _ => const [],
      },
    );
  }

  final String title;
  final String questionText;
  final String answer;
  final String analysis;
  final String subjectName;
  final List<String> tags;
}

class PracticeUiState {
  const PracticeUiState({
    this.isBusy = false,
    this.question,
    this.showAnswer = false,
    this.errorMessage,
    this.noticeMessage,
  });

  final bool isBusy;
  final PracticeQuestion? question;
  final bool showAnswer;
  final String? errorMessage;
  final String? noticeMessage;

  PracticeUiState copyWith({
    bool? isBusy,
    PracticeQuestion? question,
    bool? showAnswer,
    String? errorMessage,
    String? noticeMessage,
  }) {
    return PracticeUiState(
      isBusy: isBusy ?? this.isBusy,
      question: question ?? this.question,
      showAnswer: showAnswer ?? this.showAnswer,
      errorMessage: errorMessage,
      noticeMessage: noticeMessage,
    );
  }
}
