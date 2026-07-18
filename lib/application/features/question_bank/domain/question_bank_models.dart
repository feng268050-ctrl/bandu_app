import 'dart:math';

import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';

enum BankQuestionType {
  singleChoice,
  multipleChoice,
  fillBlank,
  subjective,
  unknown,
}

enum BankQuestionDifficulty { easy, medium, hard, challenge, unknown }

enum ExamSessionStatus { inProgress, completed }

enum ExamGradingResult { correct, incorrect, needsReview, notGraded }

enum ExamGradingSource { local, ai, manual, none }

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

  Map<BankQuestionType, int> get questionTypeCounts =>
      countBankQuestionsByType(questions);

  PdfQuestionSet copyWith({String? name}) {
    return PdfQuestionSet(
      id: id,
      name: name ?? this.name,
      sourceFileName: sourceFileName,
      localPdfPath: localPdfPath,
      questions: questions,
      importedAt: importedAt,
    );
  }
}

class ExamAttempt {
  const ExamAttempt({
    required this.id,
    required this.question,
    required this.orderIndex,
    required this.answerRevealed,
    required this.gradingResult,
    required this.gradingSource,
    this.userAnswer,
    this.gradingFeedback,
    this.submittedAt,
  });

  final String id;
  final BankQuestion question;
  final int orderIndex;
  final String? userAnswer;
  final bool answerRevealed;
  final ExamGradingResult gradingResult;
  final ExamGradingSource gradingSource;
  final String? gradingFeedback;
  final DateTime? submittedAt;

  ExamAttempt copyWith({
    String? userAnswer,
    bool? answerRevealed,
    ExamGradingResult? gradingResult,
    ExamGradingSource? gradingSource,
    String? gradingFeedback,
    DateTime? submittedAt,
  }) {
    return ExamAttempt(
      id: id,
      question: question,
      orderIndex: orderIndex,
      userAnswer: userAnswer ?? this.userAnswer,
      answerRevealed: answerRevealed ?? this.answerRevealed,
      gradingResult: gradingResult ?? this.gradingResult,
      gradingSource: gradingSource ?? this.gradingSource,
      gradingFeedback: gradingFeedback ?? this.gradingFeedback,
      submittedAt: submittedAt ?? this.submittedAt,
    );
  }
}

class ExamSession {
  const ExamSession({
    required this.id,
    required this.questionSetId,
    required this.title,
    required this.seed,
    required this.status,
    required this.attempts,
    required this.createdAt,
    this.completedAt,
  });

  final String id;
  final String questionSetId;
  final String title;
  final int seed;
  final ExamSessionStatus status;
  final List<ExamAttempt> attempts;
  final DateTime createdAt;
  final DateTime? completedAt;

  int get answeredCount =>
      attempts.where((attempt) => attempt.answerRevealed).length;

  int get correctCount => attempts
      .where((attempt) => attempt.gradingResult == ExamGradingResult.correct)
      .length;

  int get incorrectCount => attempts
      .where((attempt) => attempt.gradingResult == ExamGradingResult.incorrect)
      .length;

  int get reviewCount => attempts
      .where(
        (attempt) =>
            attempt.gradingResult == ExamGradingResult.needsReview ||
            attempt.gradingResult == ExamGradingResult.notGraded,
      )
      .length;

  Map<BankQuestionType, int> get questionTypeCounts => countBankQuestionsByType(
        attempts.map((attempt) => attempt.question),
      );

  ExamSession copyWith({
    String? title,
    ExamSessionStatus? status,
    List<ExamAttempt>? attempts,
    DateTime? completedAt,
  }) {
    return ExamSession(
      id: id,
      questionSetId: questionSetId,
      title: title ?? this.title,
      seed: seed,
      status: status ?? this.status,
      attempts: attempts ?? this.attempts,
      createdAt: createdAt,
      completedAt: completedAt ?? this.completedAt,
    );
  }
}

class ExamGrade {
  const ExamGrade({
    required this.result,
    required this.source,
    required this.feedback,
  });

  final ExamGradingResult result;
  final ExamGradingSource source;
  final String feedback;
}

ExamSession createRandomExamSession({
  required PdfQuestionSet questionSet,
  required Map<BankQuestionType, int> questionCounts,
  required String sessionId,
  required int seed,
  required DateTime createdAt,
}) {
  if (questionCounts.values.any((count) => count < 0)) {
    throw ArgumentError.value(questionCounts, 'questionCounts');
  }
  final questionCount = questionCounts.values.fold<int>(0, (a, b) => a + b);
  if (questionCount <= 0) {
    throw ArgumentError.value(questionCounts, 'questionCounts');
  }

  final availableCounts = questionSet.questionTypeCounts;
  for (final type in BankQuestionType.values) {
    if ((questionCounts[type] ?? 0) > (availableCounts[type] ?? 0)) {
      throw StateError('question_type_count_not_enough:${type.name}');
    }
  }

  final random = Random(seed);
  final selectedQuestions = <BankQuestion>[];
  for (final type in BankQuestionType.values) {
    final requestedCount = questionCounts[type] ?? 0;
    if (requestedCount == 0) continue;
    final candidates = questionSet.questions
        .where((question) => question.questionType == type)
        .toList()
      ..shuffle(random);
    selectedQuestions.addAll(candidates.take(requestedCount));
  }

  return ExamSession(
    id: sessionId,
    questionSetId: questionSet.id,
    title: '${questionSet.name} 随机考卷',
    seed: seed,
    status: ExamSessionStatus.inProgress,
    createdAt: createdAt,
    attempts: [
      for (var index = 0; index < questionCount; index++)
        ExamAttempt(
          id: '$sessionId-${index + 1}',
          question: selectedQuestions[index],
          orderIndex: index,
          answerRevealed: false,
          gradingResult: ExamGradingResult.notGraded,
          gradingSource: ExamGradingSource.none,
        ),
    ],
  );
}

Map<BankQuestionType, int> countBankQuestionsByType(
  Iterable<BankQuestion> questions,
) {
  final counts = <BankQuestionType, int>{};
  for (final question in questions) {
    counts.update(
      question.questionType,
      (count) => count + 1,
      ifAbsent: () => 1,
    );
  }
  return Map.unmodifiable(counts);
}

ExamGrade gradeBankQuestion(BankQuestion question, String userAnswer) {
  final expectedAnswer = question.answer?.trim() ?? '';
  if (expectedAnswer.isEmpty) {
    return const ExamGrade(
      result: ExamGradingResult.notGraded,
      source: ExamGradingSource.none,
      feedback: '暂无标准答案，已保留你的作答。',
    );
  }
  if (question.questionType == BankQuestionType.subjective) {
    return const ExamGrade(
      result: ExamGradingResult.needsReview,
      source: ExamGradingSource.none,
      feedback: '主观题已保留作答，需要 AI 或人工复核。',
    );
  }

  final isCorrect = question.questionType == BankQuestionType.multipleChoice
      ? _optionSet(expectedAnswer).containsAll(_optionSet(userAnswer)) &&
          _optionSet(userAnswer).containsAll(_optionSet(expectedAnswer))
      : _normalizedAnswer(expectedAnswer) == _normalizedAnswer(userAnswer);
  return ExamGrade(
    result: isCorrect ? ExamGradingResult.correct : ExamGradingResult.incorrect,
    source: ExamGradingSource.local,
    feedback: isCorrect ? '答案匹配标准答案。' : '答案与标准答案不一致。',
  );
}

String _normalizedAnswer(String value) => value
    .trim()
    .toUpperCase()
    .replaceAll(RegExp(r'\s+'), '')
    .replaceAll('，', ',')
    .replaceAll('。', '.');

Set<String> _optionSet(String value) {
  final normalized = value.trim().toUpperCase();
  if (RegExp(r'^[A-Z]+$').hasMatch(normalized)) {
    return normalized.split('').toSet();
  }
  return normalized
      .split(RegExp(r'[,，、;；\s]+'))
      .map((item) => item.trim())
      .where((item) => item.isNotEmpty)
      .toSet();
}
