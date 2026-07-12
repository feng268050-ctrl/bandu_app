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

class PracticeRecord {
  const PracticeRecord({
    required this.id,
    required this.subject,
    required this.difficulty,
    required this.isCorrect,
    required this.createdAt,
  });

  factory PracticeRecord.fromJson(Map<String, Object?> json) {
    return PracticeRecord(
      id: json['id']?.toString() ?? '',
      subject: json['subject']?.toString() ?? '未分类',
      difficulty: json['difficulty']?.toString() ?? 'medium',
      isCorrect: json['isCorrect'] == true,
      createdAt:
          DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }

  final String id;
  final String subject;
  final String difficulty;
  final bool isCorrect;
  final DateTime createdAt;
}
