import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/features/practice/domain/practice_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceApiServiceProvider = Provider<PracticeApiService>((ref) {
  return PracticeApiService(ref.watch(apiClientProvider));
});

final practiceHistoryProvider = FutureProvider<List<PracticeRecord>>((ref) {
  return ref.watch(practiceApiServiceProvider).history();
});

class PracticeApiService {
  const PracticeApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<PracticeQuestion> generate({
    required String errorItemId,
    String difficulty = 'medium',
  }) async {
    final data = await _apiClient.post<Map<String, Object?>>(
      '/practice/generate',
      data: {
        'errorItemId': errorItemId,
        'difficulty': difficulty,
        'language': 'zh',
      },
    );
    return PracticeQuestion.fromJson(data);
  }

  Future<void> record({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) async {
    await _apiClient.post<Object?>(
      '/practice/submit',
      data: {
        'subject': subject,
        'difficulty': difficulty,
        'isCorrect': isCorrect,
      },
    );
  }

  Future<List<PracticeRecord>> history({int limit = 10}) async {
    final data = await _apiClient.get<List<Object?>>(
      '/practice/history',
      queryParameters: {'limit': limit},
    );
    return data
        .whereType<Map<String, Object?>>()
        .map(PracticeRecord.fromJson)
        .toList();
  }
}
