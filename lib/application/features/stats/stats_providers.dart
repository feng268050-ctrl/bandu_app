import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final statsRepositoryProvider = Provider<StatsRepository>(
  (ref) => missingDependency('StatsRepository'),
);

final statsOverviewProvider = FutureProvider<StatsOverview>((ref) {
  return ref.watch(statsRepositoryProvider).fetchOverview();
});
