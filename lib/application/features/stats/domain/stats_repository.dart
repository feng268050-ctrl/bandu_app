import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';

abstract interface class StatsRepository {
  Future<StatsOverview> fetchOverview();
}
