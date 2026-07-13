import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';

class ApiErrorDto {
  const ApiErrorDto({required this.code, required this.message, this.details});

  factory ApiErrorDto.fromJson(JsonObject json) {
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
    if (payload is Map) {
      final envelope = requireJsonObject(payload, context: 'API envelope');
      final errorPayload = envelope['error'];
      if (errorPayload is Map) {
        final error = ApiErrorDto.fromJson(
          requireJsonObject(errorPayload, context: 'API error'),
        );
        throw AppFailure(
          code: error.code,
          message: error.message,
          statusCode: statusCode,
          details: error.details,
        );
      }

      if (envelope.containsKey('data')) {
        return envelope['data'] as T;
      }
    }
    return payload as T;
  }
}
