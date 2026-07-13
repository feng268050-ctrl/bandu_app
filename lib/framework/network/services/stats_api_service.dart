import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final statsApiServiceProvider = Provider<StatsApiService>((ref) {
  return StatsApiService(ref.watch(apiClientProvider));
});

class StatsApiService {
  const StatsApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<Map<String, Object?>> fetchOverview() {
    return _apiClient.get<Map<String, Object?>>('/stats/overview');
  }
}
