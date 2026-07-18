import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/auth_profile_cache_mapper.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/exam_session_cache_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/question_bank_cache_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/tutor_session_cache_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('error item detail survives domain-cache-json round trip', () {
    const mapper = ErrorItemCacheMapper();
    final detail = ErrorItemDetail(
      id: 'e1',
      title: '分数题',
      subjectName: '数学',
      questionText: '1/2 + 1/2',
      answer: '1',
      analysis: '同分母相加',
      masteryLevel: 2,
      updatedAt: DateTime.utc(2026, 7, 13),
    );

    final record = mapper.detailToCache(detail);
    final decoded = CachedErrorItemDetail.fromJson(record.toJson());
    final restored = mapper.detailFromCache(decoded);

    expect(restored.id, detail.id);
    expect(restored.answer, detail.answer);
    expect(restored.masteryLevel, 2);
    expect(restored.updatedAt, DateTime.utc(2026, 7, 13));
    expect(restored.isFromCache, isTrue);
  });

  test('offline auth profile survives typed cache round trip', () {
    const mapper = AuthProfileCacheMapper();
    const profile = UserProfile(
      id: 'user-1',
      email: 'student@example.com',
      name: '小伴',
      educationStage: 'junior_high',
      enrollmentYear: 2024,
    );

    final record = mapper.toCache(profile);
    final restored = mapper.fromCache(
      CachedAuthProfile.fromJson(record.toJson()),
    );

    expect(restored?.id, profile.id);
    expect(restored?.educationStage, 'junior_high');
    expect(restored?.enrollmentYear, 2024);
  });

  test('PDF question set survives typed cache round trip', () {
    const mapper = QuestionBankCacheMapper();
    final set = PdfQuestionSet(
      id: 'bank-1',
      name: '数学题集',
      sourceFileName: 'math.pdf',
      localPdfPath: '/data/math.pdf',
      importedAt: DateTime.utc(2026, 7, 13),
      questions: const [
        BankQuestion(
          id: 'q1',
          stem: '1 + 1 = ?',
          options: ['A. 1', 'B. 2'],
          answer: 'B',
          questionType: BankQuestionType.singleChoice,
          difficulty: BankQuestionDifficulty.easy,
          tags: ['加法'],
          sourcePage: 1,
          needsReview: false,
        ),
      ],
    );

    final record = mapper.toRecord(set);
    final restored =
        mapper.fromRecord(PdfQuestionSetRecord.fromJson(record.toJson()));

    expect(restored.name, '数学题集');
    expect(restored.questions.single.answer, 'B');
    expect(
        restored.questions.single.questionType, BankQuestionType.singleChoice);
  });

  test('exam session keeps random order and grading state', () {
    const mapper = ExamSessionCacheMapper();
    final now = DateTime.utc(2026, 7, 18);
    final session = ExamSession(
      id: 'exam-1',
      questionSetId: 'bank-1',
      title: '数学随机考卷',
      seed: 42,
      status: ExamSessionStatus.completed,
      createdAt: now,
      completedAt: now,
      attempts: [
        ExamAttempt(
          id: 'attempt-1',
          question: const BankQuestion(
            id: 'q1',
            stem: '1 + 1 = ?',
            options: ['A. 1', 'B. 2'],
            answer: 'B',
            questionType: BankQuestionType.singleChoice,
            difficulty: BankQuestionDifficulty.easy,
            tags: ['加法'],
            needsReview: false,
          ),
          orderIndex: 0,
          userAnswer: 'B',
          answerRevealed: true,
          gradingResult: ExamGradingResult.correct,
          gradingSource: ExamGradingSource.local,
          gradingFeedback: '答案匹配标准答案。',
          submittedAt: now,
        ),
      ],
    );

    final record = mapper.toRecord(session);
    final restored =
        mapper.fromRecord(ExamSessionRecord.fromJson(record.toJson()));

    expect(restored.title, '数学随机考卷');
    expect(restored.status, ExamSessionStatus.completed);
    expect(restored.attempts.single.question.id, 'q1');
    expect(restored.attempts.single.gradingResult, ExamGradingResult.correct);
    expect(restored.completedAt, now);
  });

  test('tutor session keeps attachments and selected question context', () {
    const mapper = TutorSessionCacheMapper();
    final now = DateTime.utc(2026, 7, 13);
    final session = TutorSession(
      id: 'session-1',
      title: '分数题',
      modelId: 'openai:one',
      createdAt: now,
      updatedAt: now,
      messages: [
        TutorMessage(
          id: 'message-1',
          role: TutorMessageRole.user,
          content: '怎么做',
          createdAt: now,
          imagePath: '/data/question.jpg',
          questionContext: const TutorQuestionContext(
            id: 'q1',
            title: '分数题',
            content: '1/2 + 1/2',
            source: '错题本',
          ),
        ),
      ],
    );

    final record = mapper.toRecord(session);
    final restored =
        mapper.fromRecord(TutorSessionRecord.fromJson(record.toJson()));

    expect(restored.modelId, 'openai:one');
    expect(restored.messages.single.imagePath, '/data/question.jpg');
    expect(restored.messages.single.questionContext?.source, '错题本');
  });
}
