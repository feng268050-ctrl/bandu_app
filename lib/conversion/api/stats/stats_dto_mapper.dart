import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class StatsDtoMapper {
  const StatsDtoMapper();

  StatsOverview overviewFromJson(Map<String, Object?> data) {
    return StatsOverview(
      totalErrors: intValue(data['totalErrors']),
      masteredCount: intValue(data['masteredCount']),
      masteryRate: doubleValue(data['masteryRate']),
      practiceTotal: intValue(data['practiceTotal']),
      practiceAccuracy: doubleValue(data['practiceAccuracy']),
    );
  }
}
