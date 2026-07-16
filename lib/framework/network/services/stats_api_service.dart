import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final statsApiServiceProvider = Provider<StatsApiService>((ref) {
  return StatsApiService(ref.watch(apiClientProvider));
});

class StatsApiService {
  const StatsApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<StatsOverviewDto> fetchOverview(StatsPeriod period) async {
    final payload = await _apiClient.get<Object?>(
      'stats/overview',
      queryParameters: {'period': period.apiValue},
    );
    return StatsOverviewDto.fromJson(
      requireJsonObject(payload, context: 'stats overview response'),
    );
  }
}
