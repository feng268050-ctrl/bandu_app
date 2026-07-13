import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/practice_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/practice_api_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('practice repository sends typed generation DTO and maps response',
      () async {
    final service = _FakePracticeApiService();
    final repository = RemotePracticeRepository(
      apiService: service,
      mapper: const PracticeDtoMapper(),
    );

    final question = await repository.generate(
      errorItemId: 'error-1',
      difficulty: 'hard',
    );

    expect(service.generationRequest?.errorItemId, 'error-1');
    expect(service.generationRequest?.difficulty, 'hard');
    expect(service.generationRequest?.language, 'zh');
    expect(question.questionText, '2 + 2');
    expect(question.tags, ['计算']);
  });

  test('practice repository records result and maps typed history DTOs',
      () async {
    final service = _FakePracticeApiService();
    final repository = RemotePracticeRepository(
      apiService: service,
      mapper: const PracticeDtoMapper(),
    );

    await repository.record(
      subject: '数学',
      difficulty: 'medium',
      isCorrect: true,
    );
    final history = await repository.history(limit: 3);

    expect(service.recordRequest?.subject, '数学');
    expect(service.recordRequest?.isCorrect, isTrue);
    expect(service.historyLimit, 3);
    expect(history.single.id, 'practice-1');
    expect(history.single.createdAt, DateTime.utc(2026, 7, 13));
  });
}

class _FakePracticeApiService implements PracticeApiService {
  GeneratePracticeRequestDto? generationRequest;
  RecordPracticeRequestDto? recordRequest;
  int? historyLimit;

  @override
  Future<PracticeQuestionDto> generate(
    GeneratePracticeRequestDto request,
  ) async {
    generationRequest = request;
    return const PracticeQuestionDto(
      title: '练习题',
      questionText: '2 + 2',
      answer: '4',
      analysis: '加法',
      subjectName: '数学',
      tags: ['计算'],
    );
  }

  @override
  Future<List<PracticeRecordDto>> history({int limit = 10}) async {
    historyLimit = limit;
    return [
      PracticeRecordDto(
        id: 'practice-1',
        subject: '数学',
        difficulty: 'medium',
        isCorrect: true,
        createdAt: DateTime.utc(2026, 7, 13),
      ),
    ];
  }

  @override
  Future<void> record(RecordPracticeRequestDto request) async {
    recordRequest = request;
  }
}
