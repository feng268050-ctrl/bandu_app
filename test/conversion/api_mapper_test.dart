import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/api/ai_config/ai_config_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/error_items/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/question_bank/pdf_question_bank_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/tutor/tutor_dto_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('AI config mapper keeps API keys write-only', () {
    const mapper = AiConfigDtoMapper();
    const draft = AiServiceConfigDraft(
      name: '辅导模型',
      baseUrl: ' https://example.com/v1 ',
      apiKey: '   ',
      model: ' model-a ',
      isDefault: true,
    );

    final config = mapper.configFromDto(
      AiServiceConfigDto.fromJson({
        'id': 'config-1',
        'name': '辅导模型',
        'baseUrl': 'https://example.com/v1',
        'model': 'model-a',
        'maskedApiKey': '••••••••',
        'hasApiKey': true,
        'isDefault': true,
      }),
    );

    expect(mapper.updateRequest(draft).toJson(), {
      'name': '辅导模型',
      'baseUrl': 'https://example.com/v1',
      'model': 'model-a',
      'isDefault': true,
    });
    expect(config.maskedApiKey, '••••••••');
    expect(config.isDefault, isTrue);
  });

  test('auth response maps into application session', () {
    const mapper = AuthDtoMapper();

    final session = mapper.sessionFromDto(
      AuthSessionDto.fromJson({
        'accessToken': 'access',
        'refreshToken': 'refresh',
        'user': {
          'id': 'u1',
          'email': 'student@example.com',
          'name': '小伴',
          'educationStage': 'primary',
          'enrollmentYear': '2022',
        },
      }),
    );

    expect(session.user.id, 'u1');
    expect(session.user.enrollmentYear, 2022);
    expect(session.accessToken, 'access');
  });

  test('error item update maps into trimmed API request', () {
    const mapper = ErrorItemDtoMapper();
    const update = ErrorItemUpdate(
      questionText: ' 1 + 1 ',
      answer: ' 2 ',
      analysis: ' 加法 ',
      masteryLevel: 3,
    );

    expect(mapper.updateRequest(update).toJson(), {
      'questionText': '1 + 1',
      'answer': '2',
      'analysis': '加法',
      'masteryLevel': 3,
    });
  });

  test('practice and stats payloads map without leaking JSON into domain', () {
    const practiceMapper = PracticeDtoMapper();
    const statsMapper = StatsDtoMapper();

    final question = practiceMapper.questionFromDto(
      PracticeQuestionDto.fromJson({
        'questionText': '题目',
        'answer': '答案',
        'analysis': '解析',
        'subjectName': '数学',
        'tags': ['计算'],
      }),
    );
    final stats = statsMapper.overviewFromDto(
      StatsOverviewDto.fromJson({
        'totalErrors': '12',
        'masteredCount': 7,
        'masteryRate': 0.58,
        'practiceTotal': 20,
        'practiceCorrect': 15,
        'practiceAccuracy': '0.75',
      }),
    );

    expect(question.subjectName, '数学');
    expect(question.tags, ['计算']);
    expect(stats.totalErrors, 12);
    expect(stats.practiceCorrect, 15);
    expect(stats.practiceAccuracy, 0.75);
  });

  test('PDF import and tutor payloads map into typed domain models', () {
    const bankMapper = PdfQuestionBankDtoMapper();
    const tutorMapper = TutorDtoMapper();

    final preview = bankMapper.previewFromDto(
      PdfImportPreviewDto.fromJson({
        'fileName': 'math.pdf',
        'totalPages': 3,
        'parser': 'ai',
        'questions': [
          {
            'stem': '1 + 1 = ?',
            'options': ['A. 1', 'B. 2'],
            'answer': 'B',
            'questionType': 'SINGLE_CHOICE',
            'difficulty': 'EASY',
            'tags': ['加法'],
            'sourcePage': 1,
            'needsReview': false,
          },
        ],
      }),
      localPdfPath: '/tmp/math.pdf',
    );
    final model = tutorMapper.modelFromDto(
      TutorModelDto.fromJson({
        'id': 'openai:one',
        'name': '辅导模型',
        'provider': 'openai',
        'model': 'model-a',
        'isDefault': true,
      }),
    );

    expect(preview.questions, hasLength(1));
    expect(preview.questions.single.options, ['A. 1', 'B. 2']);
    expect(preview.needsReviewCount, 0);
    expect(model.id, 'openai:one');
    expect(model.isDefault, isTrue);
  });
}
