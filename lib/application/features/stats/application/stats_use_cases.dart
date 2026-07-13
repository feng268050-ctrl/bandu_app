import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';

class FetchStatsOverviewUseCase {
  const FetchStatsOverviewUseCase(this._repository);

  final StatsRepository _repository;

  Future<StatsOverview> call(StatsPeriod period) {
    return _repository.fetchOverview(period);
  }
}
