import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class BankQuestionRecord {
  const BankQuestionRecord({
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

  factory BankQuestionRecord.fromJson(JsonObject json) {
    return BankQuestionRecord(
      id: json['id']?.toString() ?? '',
      stem: json['stem']?.toString() ?? '',
      options: stringListValue(json['options']),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      questionType: json['questionType']?.toString() ?? 'unknown',
      difficulty: json['difficulty']?.toString() ?? 'unknown',
      tags: stringListValue(json['tags']),
      sourcePage: nullableIntValue(json['sourcePage']),
      needsReview: boolValue(json['needsReview']),
    );
  }

  final String id;
  final String stem;
  final List<String> options;
  final String? answer;
  final String? analysis;
  final String questionType;
  final String difficulty;
  final List<String> tags;
  final int? sourcePage;
  final bool needsReview;

  JsonObject toJson() => {
        'id': id,
        'stem': stem,
        'options': options,
        'answer': answer,
        'analysis': analysis,
        'questionType': questionType,
        'difficulty': difficulty,
        'tags': tags,
        'sourcePage': sourcePage,
        'needsReview': needsReview,
      };
}

class PdfQuestionSetRecord {
  const PdfQuestionSetRecord({
    required this.id,
    required this.name,
    required this.sourceFileName,
    required this.localPdfPath,
    required this.questions,
    required this.importedAt,
  });

  factory PdfQuestionSetRecord.fromJson(JsonObject json) {
    final rawQuestions = json['questions'];
    return PdfQuestionSetRecord(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? 'PDF 题集',
      sourceFileName: json['sourceFileName']?.toString() ?? '',
      localPdfPath: json['localPdfPath']?.toString() ?? '',
      questions: rawQuestions is List
          ? rawQuestions
              .whereType<JsonObject>()
              .map(BankQuestionRecord.fromJson)
              .toList()
          : const [],
      importedAt: dateTimeValue(json['importedAt']),
    );
  }

  final String id;
  final String name;
  final String sourceFileName;
  final String localPdfPath;
  final List<BankQuestionRecord> questions;
  final DateTime importedAt;

  JsonObject toJson() => {
        'id': id,
        'name': name,
        'sourceFileName': sourceFileName,
        'localPdfPath': localPdfPath,
        'questions': questions.map((question) => question.toJson()).toList(),
        'importedAt': importedAt.toIso8601String(),
      };
}

class QuestionBankCacheMapper {
  const QuestionBankCacheMapper();

  PdfQuestionSet fromRecord(PdfQuestionSetRecord record) {
    return PdfQuestionSet(
      id: record.id,
      name: record.name,
      sourceFileName: record.sourceFileName,
      localPdfPath: record.localPdfPath,
      importedAt: record.importedAt,
      questions: record.questions.map(questionFromRecord).toList(),
    );
  }

  PdfQuestionSetRecord toRecord(PdfQuestionSet set) {
    return PdfQuestionSetRecord(
      id: set.id,
      name: set.name,
      sourceFileName: set.sourceFileName,
      localPdfPath: set.localPdfPath,
      importedAt: set.importedAt,
      questions: set.questions.map(questionToRecord).toList(),
    );
  }

  BankQuestion questionFromRecord(BankQuestionRecord record) {
    return BankQuestion(
      id: record.id,
      stem: record.stem,
      options: record.options,
      answer: record.answer,
      analysis: record.analysis,
      questionType: BankQuestionType.values.firstWhere(
        (value) => value.name == record.questionType,
        orElse: () => BankQuestionType.unknown,
      ),
      difficulty: BankQuestionDifficulty.values.firstWhere(
        (value) => value.name == record.difficulty,
        orElse: () => BankQuestionDifficulty.unknown,
      ),
      tags: record.tags,
      sourcePage: record.sourcePage,
      needsReview: record.needsReview,
    );
  }

  BankQuestionRecord questionToRecord(BankQuestion question) {
    return BankQuestionRecord(
      id: question.id,
      stem: question.stem,
      options: question.options,
      answer: question.answer,
      analysis: question.analysis,
      questionType: question.questionType.name,
      difficulty: question.difficulty.name,
      tags: question.tags,
      sourcePage: question.sourcePage,
      needsReview: question.needsReview,
    );
  }
}
