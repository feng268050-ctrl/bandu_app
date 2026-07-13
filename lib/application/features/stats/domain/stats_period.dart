enum StatsPeriod {
  today('today', '今日'),
  week('week', '本周'),
  month('month', '本月');

  const StatsPeriod(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static StatsPeriod fromApiValue(String? value) {
    return switch (value) {
      'week' => StatsPeriod.week,
      'month' => StatsPeriod.month,
      _ => StatsPeriod.today,
    };
  }
}
