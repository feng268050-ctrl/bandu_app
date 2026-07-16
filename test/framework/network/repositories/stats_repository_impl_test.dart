import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/stats_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/stats_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('stats repository maps typed overview DTO into domain model', () async {
    final service = _FakeStatsApiService();
    final repository = RemoteStatsRepository(
      apiService: service,
      mapper: const StatsDtoMapper(),
    );

    final overview = await repository.fetchOverview(StatsPeriod.week);

    expect(service.calls, 1);
    expect(overview.totalErrors, 12);
    expect(overview.masteredCount, 7);
    expect(overview.practiceAccuracy, 0.75);
  });

  test('stats repository returns marked cache when network fetch fails',
      () async {
    final service = _FakeStatsApiService();
    final cache = MemoryAppCacheDatabase();
    addTearDown(cache.dispose);
    final repository = RemoteStatsRepository(
      apiService: service,
      mapper: const StatsDtoMapper(),
      cacheDatabase: cache,
    );

    await repository.fetchOverview(StatsPeriod.week);
    service.error = const AppFailure(
      code: 'NETWORK_UNAVAILABLE',
      message: '当前网络不可用',
    );
    final cached = await repository.fetchOverview(StatsPeriod.week);

    expect(cached.isFromCache, isTrue);
    expect(cached.totalErrors, 12);
  });

  test('stats repository does not hide programming errors behind cache',
      () async {
    final service = _FakeStatsApiService();
    final cache = MemoryAppCacheDatabase();
    addTearDown(cache.dispose);
    final repository = RemoteStatsRepository(
      apiService: service,
      mapper: const StatsDtoMapper(),
      cacheDatabase: cache,
    );
    await repository.fetchOverview(StatsPeriod.week);
    service.error = StateError('invalid response mapper');

    await expectLater(
      repository.fetchOverview(StatsPeriod.week),
      throwsA(isA<StateError>()),
    );
  });
}

class _FakeStatsApiService implements StatsApiService {
  int calls = 0;
  Object? error;

  @override
  Future<StatsOverviewDto> fetchOverview(StatsPeriod period) async {
    calls += 1;
    final currentError = error;
    if (currentError != null) {
      throw currentError;
    }
    return const StatsOverviewDto(
      totalErrors: 12,
      masteredCount: 7,
      masteryRate: 7 / 12,
      practiceTotal: 20,
      practiceCorrect: 15,
      practiceAccuracy: 0.75,
    );
  }
}
