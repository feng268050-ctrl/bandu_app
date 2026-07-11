class StatsOverview {
  const StatsOverview({
    required this.totalErrors,
    required this.masteredCount,
    required this.masteryRate,
    required this.practiceTotal,
    required this.practiceAccuracy,
  });

  factory StatsOverview.fromJson(Map<String, Object?> json) {
    return StatsOverview(
      totalErrors: _asInt(json['totalErrors']),
      masteredCount: _asInt(json['masteredCount']),
      masteryRate: _asDouble(json['masteryRate']),
      practiceTotal: _asInt(json['practiceTotal']),
      practiceAccuracy: _asDouble(json['practiceAccuracy']),
    );
  }

  final int totalErrors;
  final int masteredCount;
  final double masteryRate;
  final int practiceTotal;
  final double practiceAccuracy;
}

int _asInt(Object? value) {
  return switch (value) {
    final int number => number,
    final num number => number.toInt(),
    final String text => int.tryParse(text) ?? 0,
    _ => 0,
  };
}

double _asDouble(Object? value) {
  return switch (value) {
    final double number => number,
    final num number => number.toDouble(),
    final String text => double.tryParse(text) ?? 0,
    _ => 0,
  };
}
