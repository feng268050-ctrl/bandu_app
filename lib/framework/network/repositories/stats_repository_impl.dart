import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/stats_cache_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/stats_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteStatsRepositoryProvider = Provider<StatsRepository>((ref) {
  return RemoteStatsRepository(
    apiService: ref.watch(statsApiServiceProvider),
    mapper: const StatsDtoMapper(),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    cacheMapper: const StatsCacheMapper(),
  );
});

class RemoteStatsRepository implements StatsRepository {
  const RemoteStatsRepository({
    required this.apiService,
    required this.mapper,
    this.cacheDatabase,
    this.cacheMapper = const StatsCacheMapper(),
  });

  final StatsApiService apiService;
  final StatsDtoMapper mapper;
  final AppCacheDatabase? cacheDatabase;
  final StatsCacheMapper cacheMapper;

  @override
  Future<StatsOverview> fetchOverview(StatsPeriod period) async {
    try {
      final overview = mapper.overviewFromDto(
        await apiService.fetchOverview(period),
      );
      await cacheDatabase?.upsertStatsOverview(
        cacheMapper.toCache(period, overview),
      );
      return overview;
    } catch (error) {
      if (error is! AppFailure || !error.allowsOfflineFallback) {
        rethrow;
      }
      final cached = await cacheDatabase?.readStatsOverview(period.apiValue);
      if (cached == null) {
        rethrow;
      }
      return cacheMapper.fromCache(cached);
    }
  }
}
