import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/error_items/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
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
        'practiceAccuracy': '0.75',
      }),
    );

    expect(question.subjectName, '数学');
    expect(question.tags, ['计算']);
    expect(stats.totalErrors, 12);
    expect(stats.practiceAccuracy, 0.75);
  });
}
