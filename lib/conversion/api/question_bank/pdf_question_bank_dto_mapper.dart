import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class PdfQuestionDto {
  const PdfQuestionDto({
    required this.stem,
    required this.options,
    required this.questionType,
    required this.difficulty,
    required this.tags,
    required this.needsReview,
    this.answer,
    this.analysis,
    this.sourcePage,
  });

  factory PdfQuestionDto.fromJson(JsonObject json) {
    return PdfQuestionDto(
      stem: json['stem']?.toString() ?? '',
      options: stringListValue(json['options']),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      questionType: json['questionType']?.toString() ?? 'UNKNOWN',
      difficulty: json['difficulty']?.toString() ?? 'UNKNOWN',
      tags: stringListValue(json['tags']),
      sourcePage: nullableIntValue(json['sourcePage']),
      needsReview: boolValue(json['needsReview']),
    );
  }

  final String stem;
  final List<String> options;
  final String? answer;
  final String? analysis;
  final String questionType;
  final String difficulty;
  final List<String> tags;
  final int? sourcePage;
  final bool needsReview;
}

class PdfImportPreviewDto {
  const PdfImportPreviewDto({
    required this.fileName,
    required this.totalPages,
    required this.parser,
    required this.questions,
    this.resolvedModel,
  });

  factory PdfImportPreviewDto.fromJson(JsonObject json) {
    final rawQuestions = json['questions'];
    return PdfImportPreviewDto(
      fileName: json['fileName']?.toString() ?? 'PDF 题集.pdf',
      totalPages: intValue(json['totalPages']),
      parser: json['parser']?.toString() ?? 'ai',
      questions: rawQuestions is List
          ? rawQuestions
              .whereType<JsonObject>()
              .map(PdfQuestionDto.fromJson)
              .toList()
          : const [],
      resolvedModel: _resolvedModel(json),
    );
  }

  final String fileName;
  final int totalPages;
  final String parser;
  final List<PdfQuestionDto> questions;
  final AiResolvedModel? resolvedModel;
}

class PdfQuestionBankDtoMapper {
  const PdfQuestionBankDtoMapper();

  PdfImportPreview previewFromDto(
    PdfImportPreviewDto dto, {
    required String localPdfPath,
  }) {
    return PdfImportPreview(
      fileName: dto.fileName,
      localPdfPath: localPdfPath,
      totalPages: dto.totalPages,
      parser: dto.parser,
      questions: [
        for (var index = 0; index < dto.questions.length; index++)
          _questionFromDto(dto.questions[index], index),
      ],
      resolvedModel: dto.resolvedModel,
    );
  }

  BankQuestion _questionFromDto(PdfQuestionDto dto, int index) {
    return BankQuestion(
      id: 'preview-$index',
      stem: dto.stem,
      options: dto.options,
      answer: dto.answer,
      analysis: dto.analysis,
      questionType: switch (dto.questionType) {
        'SINGLE_CHOICE' => BankQuestionType.singleChoice,
        'MULTIPLE_CHOICE' => BankQuestionType.multipleChoice,
        'FILL_BLANK' => BankQuestionType.fillBlank,
        'SUBJECTIVE' => BankQuestionType.subjective,
        _ => BankQuestionType.unknown,
      },
      difficulty: switch (dto.difficulty) {
        'EASY' => BankQuestionDifficulty.easy,
        'MEDIUM' => BankQuestionDifficulty.medium,
        'HARD' => BankQuestionDifficulty.hard,
        'CHALLENGE' => BankQuestionDifficulty.challenge,
        _ => BankQuestionDifficulty.unknown,
      },
      tags: dto.tags,
      sourcePage: dto.sourcePage,
      needsReview: dto.needsReview,
    );
  }
}

AiResolvedModel? _resolvedModel(JsonObject json) {
  final raw = json['resolvedModel'];
  if (raw is! JsonObject) return null;
  final id = raw['id']?.toString();
  final name = (raw['displayName'] ?? raw['name'] ?? raw['model'])?.toString();
  if (id == null || id.isEmpty || name == null || name.isEmpty) return null;
  return AiResolvedModel(
    id: id,
    displayName: name,
    fallbackOccurred: boolValue(
      json['fallbackOccurred'] ?? json['fallback'] ?? raw['fallbackOccurred'],
    ),
  );
}
