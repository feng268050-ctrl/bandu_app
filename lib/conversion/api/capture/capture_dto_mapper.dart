import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class CaptureDtoMapper {
  const CaptureDtoMapper();

  AnalyzeResult analyzeResultFromJson(Map<String, Object?> data) {
    return AnalyzeResult(
      title: data['title']?.toString() ?? '未命名错题',
      subjectName: data['subjectName']?.toString(),
      questionText: data['questionText']?.toString(),
      answer: data['answer']?.toString(),
      analysis: data['analysis']?.toString(),
      tags: stringListValue(data['tags']),
    );
  }

  SavedErrorItem savedErrorItemFromJson(Map<String, Object?> data) {
    return SavedErrorItem(
      id: data['id']?.toString() ?? '',
      title: data['title']?.toString() ?? '未命名错题',
      subjectName: data['subjectName']?.toString() ?? '未分类',
      questionText: data['questionText']?.toString(),
      answer: data['answer']?.toString(),
      analysis: data['analysis']?.toString(),
      updatedAt: nullableDateTimeValue(data['updatedAt']),
    );
  }

  Map<String, Object?> saveRequest(
    AnalyzeResult result, {
    required String originalImageUrl,
  }) {
    return {
      'questionText': result.questionText ?? result.title,
      'answer': result.answer,
      'analysis': result.analysis,
      'subjectName': result.subjectName,
      'tags': result.tags,
      'originalImageUrl': originalImageUrl,
    };
  }
}
