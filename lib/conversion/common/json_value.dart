typedef JsonObject = Map<String, Object?>;
typedef JsonArray = List<Object?>;

JsonObject requireJsonObject(Object? value, {String context = 'JSON value'}) {
  if (value is Map<String, Object?>) {
    return value;
  }
  if (value is Map) {
    return value.map(
      (key, item) => MapEntry(key.toString(), item),
    );
  }
  throw FormatException('$context must be a JSON object.');
}

JsonArray requireJsonArray(Object? value, {String context = 'JSON value'}) {
  if (value is List<Object?>) {
    return value;
  }
  throw FormatException('$context must be a JSON array.');
}
