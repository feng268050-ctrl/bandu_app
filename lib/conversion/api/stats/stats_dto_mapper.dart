import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class StatsOverviewDto {
  const StatsOverviewDto({
    required this.totalErrors,
    required this.masteredCount,
    required this.masteryRate,
    required this.practiceTotal,
    required this.practiceAccuracy,
  });

  factory StatsOverviewDto.fromJson(JsonObject json) {
    return StatsOverviewDto(
      totalErrors: intValue(json['totalErrors']),
      masteredCount: intValue(json['masteredCount']),
      masteryRate: doubleValue(json['masteryRate']),
      practiceTotal: intValue(json['practiceTotal']),
      practiceAccuracy: doubleValue(json['practiceAccuracy']),
    );
  }

  final int totalErrors;
  final int masteredCount;
  final double masteryRate;
  final int practiceTotal;
  final double practiceAccuracy;
}

class StatsDtoMapper {
  const StatsDtoMapper();

  StatsOverview overviewFromDto(StatsOverviewDto dto) {
    return StatsOverview(
      totalErrors: dto.totalErrors,
      masteredCount: dto.masteredCount,
      masteryRate: dto.masteryRate,
      practiceTotal: dto.practiceTotal,
      practiceAccuracy: dto.practiceAccuracy,
    );
  }
}
