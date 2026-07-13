import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/practice_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remotePracticeRepositoryProvider = Provider<PracticeRepository>((ref) {
  return RemotePracticeRepository(
    apiService: ref.watch(practiceApiServiceProvider),
    mapper: const PracticeDtoMapper(),
  );
});

class RemotePracticeRepository implements PracticeRepository {
  const RemotePracticeRepository({
    required this.apiService,
    required this.mapper,
  });

  final PracticeApiService apiService;
  final PracticeDtoMapper mapper;

  @override
  Future<PracticeQuestion> generate({
    required String errorItemId,
    String difficulty = 'medium',
  }) async {
    final data = await apiService.generate(
      mapper.generateRequest(
        errorItemId: errorItemId,
        difficulty: difficulty,
      ),
    );
    return mapper.questionFromJson(data);
  }

  @override
  Future<void> record({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) {
    return apiService.record(
      mapper.recordRequest(
        subject: subject,
        difficulty: difficulty,
        isCorrect: isCorrect,
      ),
    );
  }

  @override
  Future<List<PracticeRecord>> history({int limit = 10}) async {
    return mapper.historyFromJson(await apiService.history(limit: limit));
  }
}
