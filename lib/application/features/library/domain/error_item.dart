class ErrorItemSummary {
  const ErrorItemSummary({
    required this.id,
    required this.title,
    required this.subjectName,
    required this.updatedAt,
    this.mastered = false,
  });

  final String id;
  final String title;
  final String subjectName;
  final DateTime updatedAt;
  final bool mastered;
}

class ErrorItemDetail {
  const ErrorItemDetail({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.masteryLevel = 0,
    this.updatedAt,
  });

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final int masteryLevel;
  final DateTime? updatedAt;
}

class ErrorItemUpdate {
  const ErrorItemUpdate({
    required this.questionText,
    required this.answer,
    required this.analysis,
    required this.masteryLevel,
  });

  final String questionText;
  final String answer;
  final String analysis;
  final int masteryLevel;
}
