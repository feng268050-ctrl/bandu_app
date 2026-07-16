import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';

class StatsCacheMapper {
  const StatsCacheMapper();

  CachedStatsOverview toCache(StatsPeriod period, StatsOverview overview) {
    return CachedStatsOverview(
      period: period.apiValue,
      totalErrors: overview.totalErrors,
      masteredCount: overview.masteredCount,
      masteryRate: overview.masteryRate,
      practiceTotal: overview.practiceTotal,
      practiceCorrect: overview.practiceCorrect,
      practiceAccuracy: overview.practiceAccuracy,
      cachedAt: DateTime.now(),
    );
  }

  StatsOverview fromCache(CachedStatsOverview cached) {
    return StatsOverview(
      totalErrors: cached.totalErrors,
      masteredCount: cached.masteredCount,
      masteryRate: cached.masteryRate,
      practiceTotal: cached.practiceTotal,
      practiceCorrect: cached.practiceCorrect,
      practiceAccuracy: cached.practiceAccuracy,
      isFromCache: true,
    );
  }
}
