enum StatsMetric {
  errors('errors'),
  mastered('mastered'),
  accuracy('accuracy');

  const StatsMetric(this.routeValue);

  final String routeValue;

  static StatsMetric fromRouteValue(String? value) {
    return switch (value) {
      'mastered' => StatsMetric.mastered,
      'accuracy' => StatsMetric.accuracy,
      _ => StatsMetric.errors,
    };
  }
}
