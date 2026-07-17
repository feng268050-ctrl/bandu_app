import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class AnalyzeResultDto {
  const AnalyzeResultDto({
    required this.title,
    this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.tags = const [],
    this.resolvedModel,
  });

  factory AnalyzeResultDto.fromJson(JsonObject json) {
    return AnalyzeResultDto(
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString(),
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      tags: stringListValue(json['tags']),
      resolvedModel: _resolvedModel(json),
    );
  }

  final String title;
  final String? subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final List<String> tags;
  final AiResolvedModel? resolvedModel;
}

class SavedErrorItemDto {
  const SavedErrorItemDto({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.updatedAt,
  });

  factory SavedErrorItemDto.fromJson(JsonObject json) {
    return SavedErrorItemDto(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      updatedAt: nullableDateTimeValue(json['updatedAt']),
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final DateTime? updatedAt;
}

class SaveAnalyzedCaptureRequestDto {
  const SaveAnalyzedCaptureRequestDto({
    required this.questionText,
    required this.tags,
    required this.originalImageUrl,
    this.answer,
    this.analysis,
    this.subjectName,
  });

  final String questionText;
  final String? answer;
  final String? analysis;
  final String? subjectName;
  final List<String> tags;
  final String originalImageUrl;

  JsonObject toJson() => {
        'questionText': questionText,
        'answer': answer,
        'analysis': analysis,
        'subjectName': subjectName,
        'tags': tags,
        'originalImageUrl': originalImageUrl,
      };
}

class CaptureDtoMapper {
  const CaptureDtoMapper();

  AnalyzeResult analyzeResultFromDto(AnalyzeResultDto dto) {
    return AnalyzeResult(
      title: dto.title,
      subjectName: dto.subjectName,
      questionText: dto.questionText,
      answer: dto.answer,
      analysis: dto.analysis,
      tags: dto.tags,
      resolvedModel: dto.resolvedModel,
    );
  }

  SavedErrorItem savedErrorItemFromDto(SavedErrorItemDto dto) {
    return SavedErrorItem(
      id: dto.id,
      title: dto.title,
      subjectName: dto.subjectName,
      questionText: dto.questionText,
      answer: dto.answer,
      analysis: dto.analysis,
      updatedAt: dto.updatedAt,
    );
  }

  SaveAnalyzedCaptureRequestDto saveRequest(
    AnalyzeResult result, {
    required String originalImageUrl,
  }) {
    return SaveAnalyzedCaptureRequestDto(
      questionText: result.questionText ?? result.title,
      answer: result.answer,
      analysis: result.analysis,
      subjectName: result.subjectName,
      tags: result.tags,
      originalImageUrl: originalImageUrl,
    );
  }
}

AiResolvedModel? _resolvedModel(JsonObject json) {
  final raw = json['resolvedModel'];
  if (raw is! JsonObject) return null;
  final id = raw['id']?.toString();
  final displayName =
      (raw['displayName'] ?? raw['name'] ?? raw['model'])?.toString();
  if (id == null || id.isEmpty || displayName == null || displayName.isEmpty) {
    return null;
  }
  return AiResolvedModel(
    id: id,
    displayName: displayName,
    fallbackOccurred: boolValue(
      json['fallbackOccurred'] ?? json['fallback'] ?? raw['fallbackOccurred'],
    ),
  );
}
