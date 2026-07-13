int intValue(Object? value, {int fallback = 0}) {
  return switch (value) {
    final int number => number,
    final num number => number.toInt(),
    final String text => int.tryParse(text) ?? fallback,
    _ => fallback,
  };
}

int? nullableIntValue(Object? value) {
  if (value == null) {
    return null;
  }
  return switch (value) {
    final int number => number,
    final num number => number.toInt(),
    final String text => int.tryParse(text),
    _ => null,
  };
}

bool boolValue(Object? value, {bool fallback = false}) {
  return switch (value) {
    final bool flag => flag,
    final num number => number != 0,
    final String text when text.toLowerCase() == 'true' => true,
    final String text when text.toLowerCase() == 'false' => false,
    _ => fallback,
  };
}

double doubleValue(Object? value, {double fallback = 0}) {
  return switch (value) {
    final double number => number,
    final num number => number.toDouble(),
    final String text => double.tryParse(text) ?? fallback,
    _ => fallback,
  };
}

DateTime? nullableDateTimeValue(Object? value) {
  return DateTime.tryParse(value?.toString() ?? '');
}

DateTime dateTimeValue(
  Object? value, {
  DateTime? fallback,
}) {
  return nullableDateTimeValue(value) ??
      fallback ??
      DateTime.fromMillisecondsSinceEpoch(0);
}

List<String> stringListValue(Object? value) {
  return switch (value) {
    final List<Object?> values =>
      values.map((item) => item.toString()).toList(),
    _ => const [],
  };
}
