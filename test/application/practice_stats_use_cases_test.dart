import 'package:bandu_wrong_notebook/application/features/practice/application/practice_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';
import 'package:bandu_wrong_notebook/application/features/stats/application/stats_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('practice use cases own repository interactions', () async {
    final repository = _FakePracticeRepository();

    final question = await GeneratePracticeUseCase(repository)(
      errorItemId: 'error-1',
      difficulty: 'hard',
    );
    await RecordPracticeResultUseCase(repository)(
      subject: question.subjectName,
      difficulty: 'hard',
      isCorrect: true,
    );
    final history = await FetchPracticeHistoryUseCase(repository)(limit: 5);

    expect(repository.generatedErrorItemId, 'error-1');
    expect(repository.recordedCorrect, isTrue);
    expect(repository.historyLimit, 5);
    expect(history.single.id, 'history-1');
  });

  test('stats use case fetches overview from repository', () async {
    final repository = _FakeStatsRepository();

    final overview = await FetchStatsOverviewUseCase(repository)();

    expect(repository.calls, 1);
    expect(overview.totalErrors, 4);
  });
}

class _FakePracticeRepository implements PracticeRepository {
  String? generatedErrorItemId;
  bool? recordedCorrect;
  int? historyLimit;

  @override
  Future<PracticeQuestion> generate({
    required String errorItemId,
    String difficulty = 'medium',
  }) async {
    generatedErrorItemId = errorItemId;
    return const PracticeQuestion(
      title: '练习题',
      questionText: '题目',
      answer: '答案',
      analysis: '解析',
      subjectName: '数学',
    );
  }

  @override
  Future<List<PracticeRecord>> history({int limit = 10}) async {
    historyLimit = limit;
    return [
      PracticeRecord(
        id: 'history-1',
        subject: '数学',
        difficulty: 'hard',
        isCorrect: true,
        createdAt: DateTime.utc(2026, 7, 13),
      ),
    ];
  }

  @override
  Future<void> record({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) async {
    recordedCorrect = isCorrect;
  }
}

class _FakeStatsRepository implements StatsRepository {
  int calls = 0;

  @override
  Future<StatsOverview> fetchOverview() async {
    calls += 1;
    return const StatsOverview(
      totalErrors: 4,
      masteredCount: 2,
      masteryRate: 0.5,
      practiceTotal: 3,
      practiceAccuracy: 2 / 3,
    );
  }
}
