import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class ErrorItemDtoMapper {
  const ErrorItemDtoMapper();

  List<ErrorItemSummary> summaryListFromJson(List<Object?> data) {
    return data.whereType<Map<String, Object?>>().map(summaryFromJson).toList();
  }

  ErrorItemSummary summaryFromJson(Map<String, Object?> data) {
    return ErrorItemSummary(
      id: data['id']?.toString() ?? '',
      title: data['title']?.toString() ?? '未命名错题',
      subjectName: data['subjectName']?.toString() ?? '未分类',
      updatedAt: dateTimeValue(data['updatedAt']),
      mastered: data['mastered'] == true,
    );
  }

  ErrorItemDetail detailFromJson(Map<String, Object?> data) {
    return ErrorItemDetail(
      id: data['id']?.toString() ?? '',
      title: data['title']?.toString() ?? '未命名错题',
      subjectName: data['subjectName']?.toString() ?? '未分类',
      questionText: data['questionText']?.toString(),
      answer: data['answer']?.toString(),
      analysis: data['analysis']?.toString(),
      masteryLevel: intValue(data['masteryLevel']),
      updatedAt: nullableDateTimeValue(data['updatedAt']),
    );
  }

  Map<String, Object?> updateRequest(ErrorItemUpdate update) {
    return {
      'questionText': update.questionText.trim(),
      'answer': update.answer.trim(),
      'analysis': update.analysis.trim(),
      'masteryLevel': update.masteryLevel,
    };
  }
}
