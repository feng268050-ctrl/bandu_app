import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';

enum BankQuestionType {
  singleChoice,
  multipleChoice,
  fillBlank,
  subjective,
  unknown,
}

enum BankQuestionDifficulty { easy, medium, hard, challenge, unknown }

class BankQuestion {
  const BankQuestion({
    required this.id,
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

  final String id;
  final String stem;
  final List<String> options;
  final String? answer;
  final String? analysis;
  final BankQuestionType questionType;
  final BankQuestionDifficulty difficulty;
  final List<String> tags;
  final int? sourcePage;
  final bool needsReview;

  String get tutorContext {
    final buffer = StringBuffer(stem);
    if (options.isNotEmpty) {
      buffer.write('\n${options.join('\n')}');
    }
    if (answer?.isNotEmpty == true) {
      buffer.write('\n参考答案：$answer');
    }
    return buffer.toString();
  }
}

class PdfImportPreview {
  const PdfImportPreview({
    required this.fileName,
    required this.localPdfPath,
    required this.totalPages,
    required this.parser,
    required this.questions,
    this.resolvedModel,
  });

  final String fileName;
  final String localPdfPath;
  final int totalPages;
  final String parser;
  final List<BankQuestion> questions;
  final AiResolvedModel? resolvedModel;

  int get needsReviewCount =>
      questions.where((question) => question.needsReview).length;

  PdfImportPreview copyWith({List<BankQuestion>? questions}) {
    return PdfImportPreview(
      fileName: fileName,
      localPdfPath: localPdfPath,
      totalPages: totalPages,
      parser: parser,
      questions: questions ?? this.questions,
      resolvedModel: resolvedModel,
    );
  }
}

class PdfQuestionSet {
  const PdfQuestionSet({
    required this.id,
    required this.name,
    required this.sourceFileName,
    required this.localPdfPath,
    required this.questions,
    required this.importedAt,
  });

  final String id;
  final String name;
  final String sourceFileName;
  final String localPdfPath;
  final List<BankQuestion> questions;
  final DateTime importedAt;
}
