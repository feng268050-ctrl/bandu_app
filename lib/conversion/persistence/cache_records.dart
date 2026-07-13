import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class CachedErrorItem {
  const CachedErrorItem({
    required this.id,
    required this.title,
    required this.subjectName,
    required this.updatedAt,
    this.mastered = false,
  });

  factory CachedErrorItem.fromJson(JsonObject json) {
    return CachedErrorItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      updatedAt: dateTimeValue(json['updatedAt']),
      mastered: json['mastered'] == true,
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final DateTime updatedAt;
  final bool mastered;

  JsonObject toJson() {
    return {
      'id': id,
      'title': title,
      'subjectName': subjectName,
      'updatedAt': updatedAt.toIso8601String(),
      'mastered': mastered,
    };
  }
}

class CachedErrorItemDetail {
  const CachedErrorItemDetail({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.masteryLevel = 0,
    this.updatedAt,
  });

  factory CachedErrorItemDetail.fromJson(JsonObject json) {
    return CachedErrorItemDetail(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      masteryLevel: intValue(json['masteryLevel']),
      updatedAt: nullableDateTimeValue(json['updatedAt']),
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final int masteryLevel;
  final DateTime? updatedAt;

  JsonObject toJson() {
    return {
      'id': id,
      'title': title,
      'subjectName': subjectName,
      'questionText': questionText,
      'answer': answer,
      'analysis': analysis,
      'masteryLevel': masteryLevel,
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}
