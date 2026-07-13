import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/stats_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/stats_api_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('stats repository maps typed overview DTO into domain model', () async {
    final service = _FakeStatsApiService();
    final repository = RemoteStatsRepository(
      apiService: service,
      mapper: const StatsDtoMapper(),
    );

    final overview = await repository.fetchOverview();

    expect(service.calls, 1);
    expect(overview.totalErrors, 12);
    expect(overview.masteredCount, 7);
    expect(overview.practiceAccuracy, 0.75);
  });
}

class _FakeStatsApiService implements StatsApiService {
  int calls = 0;

  @override
  Future<StatsOverviewDto> fetchOverview() async {
    calls += 1;
    return const StatsOverviewDto(
      totalErrors: 12,
      masteredCount: 7,
      masteryRate: 7 / 12,
      practiceTotal: 20,
      practiceAccuracy: 0.75,
    );
  }
}
