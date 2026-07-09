class ErrorItemSummary {
  const ErrorItemSummary({
    required this.id,
    required this.title,
    required this.subjectName,
    required this.updatedAt,
    this.mastered = false,
  });

  factory ErrorItemSummary.fromJson(Map<String, Object?> json) {
    return ErrorItemSummary(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      mastered: json['mastered'] == true,
    );
  }

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
  });

  factory ErrorItemDetail.fromJson(Map<String, Object?> json) {
    return ErrorItemDetail(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
}
