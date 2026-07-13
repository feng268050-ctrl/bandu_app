import 'package:bandu_wrong_notebook/conversion/api/practice/practice_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceApiServiceProvider = Provider<PracticeApiService>((ref) {
  return PracticeApiService(ref.watch(apiClientProvider));
});

class PracticeApiService {
  const PracticeApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<PracticeQuestionDto> generate(
    GeneratePracticeRequestDto request,
  ) async {
    final payload = await _apiClient.post<Object?>(
      '/practice/generate',
      data: request.toJson(),
    );
    return PracticeQuestionDto.fromJson(
      requireJsonObject(payload, context: 'practice generation response'),
    );
  }

  Future<void> record(RecordPracticeRequestDto request) async {
    await _apiClient.post<Object?>(
      '/practice/submit',
      data: request.toJson(),
    );
  }

  Future<List<PracticeRecordDto>> history({int limit = 10}) async {
    final payload = await _apiClient.get<Object?>(
      '/practice/history',
      queryParameters: {'limit': limit},
    );
    return requireJsonArray(payload, context: 'practice history response')
        .map(
          (item) => PracticeRecordDto.fromJson(
            requireJsonObject(item, context: 'practice history item'),
          ),
        )
        .toList();
  }
}
