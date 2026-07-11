import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/features/practice/domain/practice_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceApiServiceProvider = Provider<PracticeApiService>((ref) {
  return PracticeApiService(ref.watch(apiClientProvider));
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
      '/practice/records',
      data: {
        'subject': subject,
        'difficulty': difficulty,
        'isCorrect': isCorrect,
      },
    );
  }
}
