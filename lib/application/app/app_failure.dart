class AppFailure implements Exception {
  const AppFailure({
    required this.code,
    required this.message,
    this.statusCode,
    this.details,
  });

  final String code;
  final String message;
  final int? statusCode;
  final Object? details;

  String get userMessage => message;

  bool get isCancellation => code == 'REQUEST_CANCELLED';

  bool get allowsOfflineFallback =>
      code == 'NETWORK_UNAVAILABLE' ||
      code == 'DNS_LOOKUP_FAILED' ||
      code == 'CONNECTION_TIMEOUT' ||
      code == 'RECEIVE_TIMEOUT' ||
      code == 'RESPONSE_PROCESSING_TIMEOUT' ||
      code == 'TLS_CERTIFICATE_ERROR' ||
      statusCode == 408 ||
      statusCode == 429 ||
      statusCode == 500 ||
      statusCode == 502 ||
      statusCode == 503 ||
      statusCode == 504;

  @override
  String toString() => message;
}

String appFailureUserMessage(Object error) {
  if (error is AppFailure) {
    return error.userMessage;
  }
  return '操作失败，请稍后重试。';
}
