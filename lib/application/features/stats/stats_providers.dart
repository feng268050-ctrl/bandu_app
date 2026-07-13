import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/stats/application/stats_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final statsRepositoryProvider = Provider<StatsRepository>(
  (ref) => missingDependency('StatsRepository'),
);

final fetchStatsOverviewUseCaseProvider = Provider<FetchStatsOverviewUseCase>((
  ref,
) {
  return FetchStatsOverviewUseCase(ref.watch(statsRepositoryProvider));
});

final statsOverviewProvider =
    FutureProvider.family<StatsOverview, StatsPeriod>((
  ref,
  period,
) {
  return ref.watch(fetchStatsOverviewUseCaseProvider).call(period);
});
