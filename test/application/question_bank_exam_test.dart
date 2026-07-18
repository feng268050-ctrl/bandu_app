import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final questionSet = PdfQuestionSet(
    id: 'bank-1',
    name: '数学题集',
    sourceFileName: 'math.pdf',
    localPdfPath: '/data/math.pdf',
    importedAt: DateTime.utc(2026, 7, 18),
    questions: [
      for (var index = 0; index < 8; index++)
        BankQuestion(
          id: 'q-$index',
          stem: '第 $index 题',
          options: const ['A. 1', 'B. 2'],
          answer: 'B',
          questionType: BankQuestionType.singleChoice,
          difficulty: BankQuestionDifficulty.medium,
          tags: const [],
          needsReview: false,
        ),
    ],
  );

  test('random exam keeps requested count and stable seeded order', () {
    final createdAt = DateTime.utc(2026, 7, 18);
    final first = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {BankQuestionType.singleChoice: 5},
      sessionId: 'exam-1',
      seed: 42,
      createdAt: createdAt,
    );
    final second = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {BankQuestionType.singleChoice: 5},
      sessionId: 'exam-2',
      seed: 42,
      createdAt: createdAt,
    );

    expect(first.attempts, hasLength(5));
    expect(
      first.attempts.map((attempt) => attempt.question.id),
      second.attempts.map((attempt) => attempt.question.id),
    );
    expect(first.attempts.map((attempt) => attempt.question.id).toSet(),
        hasLength(5));
  });

  test('random exam rejects an unavailable question count', () {
    expect(
      () => createRandomExamSession(
        questionSet: questionSet,
        questionCounts: const {BankQuestionType.singleChoice: 9},
        sessionId: 'exam-1',
        seed: 42,
        createdAt: DateTime.utc(2026, 7, 18),
      ),
      throwsA(isA<StateError>()),
    );
  });

  test('random exam keeps the requested count for every detected type', () {
    final mixedQuestionSet = PdfQuestionSet(
      id: 'bank-mixed',
      name: '混合题集',
      sourceFileName: 'mixed.pdf',
      localPdfPath: '/data/mixed.pdf',
      importedAt: DateTime.utc(2026, 7, 18),
      questions: [
        ..._questionsOfType(BankQuestionType.singleChoice, 3, 'single'),
        ..._questionsOfType(BankQuestionType.multipleChoice, 4, 'multiple'),
        ..._questionsOfType(BankQuestionType.fillBlank, 2, 'fill'),
        ..._questionsOfType(BankQuestionType.subjective, 2, 'subjective'),
      ],
    );

    final session = createRandomExamSession(
      questionSet: mixedQuestionSet,
      questionCounts: const {
        BankQuestionType.singleChoice: 2,
        BankQuestionType.multipleChoice: 3,
        BankQuestionType.fillBlank: 1,
        BankQuestionType.subjective: 1,
      },
      sessionId: 'exam-mixed',
      seed: 42,
      createdAt: DateTime.utc(2026, 7, 18),
    );

    expect(session.attempts, hasLength(7));
    expect(session.questionTypeCounts, {
      BankQuestionType.singleChoice: 2,
      BankQuestionType.multipleChoice: 3,
      BankQuestionType.fillBlank: 1,
      BankQuestionType.subjective: 1,
    });
    expect(
      session.attempts.map((attempt) => attempt.question.questionType),
      [
        BankQuestionType.singleChoice,
        BankQuestionType.singleChoice,
        BankQuestionType.multipleChoice,
        BankQuestionType.multipleChoice,
        BankQuestionType.multipleChoice,
        BankQuestionType.fillBlank,
        BankQuestionType.subjective,
      ],
    );
  });

  test('objective grading normalizes single and multiple choice answers', () {
    final single = gradeBankQuestion(questionSet.questions.first, ' b ');
    const multipleQuestion = BankQuestion(
      id: 'multi',
      stem: '多选题',
      options: ['A', 'B', 'C'],
      answer: 'A、C',
      questionType: BankQuestionType.multipleChoice,
      difficulty: BankQuestionDifficulty.medium,
      tags: [],
      needsReview: false,
    );
    final multiple = gradeBankQuestion(multipleQuestion, 'c, a');

    expect(single.result, ExamGradingResult.correct);
    expect(single.source, ExamGradingSource.local);
    expect(multiple.result, ExamGradingResult.correct);
  });

  test('subjective and unanswered-reference questions are not guessed', () {
    const subjectiveQuestion = BankQuestion(
      id: 'subjective',
      stem: '说明原因',
      options: [],
      answer: '参考说明',
      questionType: BankQuestionType.subjective,
      difficulty: BankQuestionDifficulty.hard,
      tags: [],
      needsReview: false,
    );
    const noAnswerQuestion = BankQuestion(
      id: 'no-answer',
      stem: '开放题',
      options: [],
      questionType: BankQuestionType.unknown,
      difficulty: BankQuestionDifficulty.unknown,
      tags: [],
      needsReview: true,
    );

    expect(
      gradeBankQuestion(subjectiveQuestion, '我的说明').result,
      ExamGradingResult.needsReview,
    );
    expect(
      gradeBankQuestion(noAnswerQuestion, '我的答案').result,
      ExamGradingResult.notGraded,
    );
  });
}

List<BankQuestion> _questionsOfType(
  BankQuestionType type,
  int count,
  String idPrefix,
) {
  return [
    for (var index = 0; index < count; index++)
      BankQuestion(
        id: '$idPrefix-$index',
        stem: '$idPrefix $index',
        options: type == BankQuestionType.singleChoice ||
                type == BankQuestionType.multipleChoice
            ? const ['A. 1', 'B. 2']
            : const [],
        answer: 'B',
        questionType: type,
        difficulty: BankQuestionDifficulty.medium,
        tags: const [],
        needsReview: false,
      ),
  ];
}
