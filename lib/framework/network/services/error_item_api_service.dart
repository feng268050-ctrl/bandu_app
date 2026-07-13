import 'package:bandu_wrong_notebook/conversion/api/error_items/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemApiServiceProvider = Provider<ErrorItemApiService>((ref) {
  return ErrorItemApiService(ref.watch(apiClientProvider));
});

class ErrorItemApiService {
  const ErrorItemApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<ErrorItemSummaryDto>> fetchErrorItems() async {
    final payload = await _apiClient.get<Object?>('/error-items');
    return requireJsonArray(payload, context: 'error item list response')
        .map(
          (item) => ErrorItemSummaryDto.fromJson(
            requireJsonObject(item, context: 'error item summary'),
          ),
        )
        .toList();
  }

  Future<ErrorItemDetailDto> fetchErrorItem(String id) async {
    final payload = await _apiClient.get<Object?>('/error-items/$id');
    return ErrorItemDetailDto.fromJson(
      requireJsonObject(payload, context: 'error item detail response'),
    );
  }

  Future<ErrorItemDetailDto> updateErrorItem(
    String id,
    ErrorItemUpdateRequestDto request,
  ) async {
    final payload = await _apiClient.patch<Object?>(
      '/error-items/$id',
      data: request.toJson(),
    );
    return ErrorItemDetailDto.fromJson(
      requireJsonObject(payload, context: 'updated error item response'),
    );
  }

  Future<void> deleteErrorItem(String id) async {
    await _apiClient.delete<Object?>('/error-items/$id');
  }
}
