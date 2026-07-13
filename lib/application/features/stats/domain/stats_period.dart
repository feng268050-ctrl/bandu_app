enum StatsPeriod {
  week('week'),
  month('month');

  const StatsPeriod(this.apiValue);

  final String apiValue;

  static StatsPeriod fromApiValue(String? value) {
    return switch (value) {
      'month' => StatsPeriod.month,
      _ => StatsPeriod.week,
    };
  }
}
