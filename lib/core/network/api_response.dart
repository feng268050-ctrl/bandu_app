class ApiEnvelope<T> {
  const ApiEnvelope({required this.data, this.error, this.meta});

  final T? data;
  final ApiError? error;
  final Map<String, Object?>? meta;
}

class ApiError {
  const ApiError({required this.code, required this.message, this.details});

  factory ApiError.fromJson(Map<String, Object?> json) {
    return ApiError(
      code: json['code']?.toString() ?? 'UNKNOWN_ERROR',
      message: json['message']?.toString() ?? '请求失败',
      details: json['details'],
    );
  }

  final String code;
  final String message;
  final Object? details;
}
