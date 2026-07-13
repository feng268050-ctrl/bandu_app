import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class ErrorItemSummaryDto {
  const ErrorItemSummaryDto({
    required this.id,
    required this.title,
    required this.subjectName,
    required this.updatedAt,
    required this.mastered,
  });

  factory ErrorItemSummaryDto.fromJson(JsonObject json) {
    return ErrorItemSummaryDto(
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
}

class ErrorItemDetailDto {
  const ErrorItemDetailDto({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.masteryLevel = 0,
    this.updatedAt,
  });

  factory ErrorItemDetailDto.fromJson(JsonObject json) {
    return ErrorItemDetailDto(
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
}

class ErrorItemUpdateRequestDto {
  const ErrorItemUpdateRequestDto({
    required this.questionText,
    required this.answer,
    required this.analysis,
    required this.masteryLevel,
  });

  final String questionText;
  final String answer;
  final String analysis;
  final int masteryLevel;

  JsonObject toJson() => {
        'questionText': questionText,
        'answer': answer,
        'analysis': analysis,
        'masteryLevel': masteryLevel,
      };
}

class ErrorItemDtoMapper {
  const ErrorItemDtoMapper();

  List<ErrorItemSummary> summaryListFromDtos(
    List<ErrorItemSummaryDto> dtos,
  ) {
    return dtos.map(summaryFromDto).toList();
  }

  ErrorItemSummary summaryFromDto(ErrorItemSummaryDto dto) {
    return ErrorItemSummary(
      id: dto.id,
      title: dto.title,
      subjectName: dto.subjectName,
      updatedAt: dto.updatedAt,
      mastered: dto.mastered,
    );
  }

  ErrorItemDetail detailFromDto(ErrorItemDetailDto dto) {
    return ErrorItemDetail(
      id: dto.id,
      title: dto.title,
      subjectName: dto.subjectName,
      questionText: dto.questionText,
      answer: dto.answer,
      analysis: dto.analysis,
      masteryLevel: dto.masteryLevel,
      updatedAt: dto.updatedAt,
    );
  }

  ErrorItemUpdateRequestDto updateRequest(ErrorItemUpdate update) {
    return ErrorItemUpdateRequestDto(
      questionText: update.questionText.trim(),
      answer: update.answer.trim(),
      analysis: update.analysis.trim(),
      masteryLevel: update.masteryLevel,
    );
  }
}
