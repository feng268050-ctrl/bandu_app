import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class PracticeDtoMapper {
  const PracticeDtoMapper();

  Map<String, Object?> generateRequest({
    required String errorItemId,
    required String difficulty,
  }) {
    return {
      'errorItemId': errorItemId,
      'difficulty': difficulty,
      'language': 'zh',
    };
  }

  Map<String, Object?> recordRequest({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) {
    return {
      'subject': subject,
      'difficulty': difficulty,
      'isCorrect': isCorrect,
    };
  }

  PracticeQuestion questionFromJson(Map<String, Object?> data) {
    return PracticeQuestion(
      title: data['title']?.toString() ?? '练习题',
      questionText: data['questionText']?.toString() ?? '',
      answer: data['answer']?.toString() ?? '',
      analysis: data['analysis']?.toString() ?? '',
      subjectName: data['subjectName']?.toString() ?? '其他',
      tags: stringListValue(data['tags']),
    );
  }

  List<PracticeRecord> historyFromJson(List<Object?> data) {
    return data.whereType<Map<String, Object?>>().map((item) {
      return PracticeRecord(
        id: item['id']?.toString() ?? '',
        subject: item['subject']?.toString() ?? '未分类',
        difficulty: item['difficulty']?.toString() ?? 'medium',
        isCorrect: item['isCorrect'] == true,
        createdAt: dateTimeValue(item['createdAt']),
      );
    }).toList();
  }
}
