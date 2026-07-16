import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/auth_profile_cache_mapper.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
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
