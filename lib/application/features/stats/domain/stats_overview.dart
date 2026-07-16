class StatsOverview {
  const StatsOverview({
    required this.totalErrors,
    required this.masteredCount,
    required this.masteryRate,
    required this.practiceTotal,
    required this.practiceCorrect,
    required this.practiceAccuracy,
    this.isFromCache = false,
  });

  final int totalErrors;
  final int masteredCount;
  final double masteryRate;
  final int practiceTotal;
  final int practiceCorrect;
  final double practiceAccuracy;
  final bool isFromCache;
}
