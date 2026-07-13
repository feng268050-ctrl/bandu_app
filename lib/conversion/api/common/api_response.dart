import 'package:bandu_wrong_notebook/application/app/app_failure.dart';

class ApiErrorDto {
  const ApiErrorDto({required this.code, required this.message, this.details});

  factory ApiErrorDto.fromJson(Map<String, Object?> json) {
    return ApiErrorDto(
      code: json['code']?.toString() ?? 'UNKNOWN_ERROR',
      message: json['message']?.toString() ?? '请求失败',
      details: json['details'],
    );
  }

  final String code;
  final String message;
  final Object? details;
}

class ApiEnvelopeMapper {
  const ApiEnvelopeMapper();

  T unwrap<T>(Object? payload, {int? statusCode}) {
    if (payload is Map<String, Object?>) {
      final errorPayload = payload['error'];
      if (errorPayload is Map<String, Object?>) {
        final error = ApiErrorDto.fromJson(errorPayload);
        throw AppFailure(
          code: error.code,
          message: error.message,
          statusCode: statusCode,
          details: error.details,
        );
      }

      if (payload.containsKey('data')) {
        return payload['data'] as T;
      }
    }
    return payload as T;
  }
}
