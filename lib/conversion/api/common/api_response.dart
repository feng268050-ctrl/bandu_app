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
          message: _apiErrorUserMessage(error, statusCode),
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

String _apiErrorUserMessage(ApiErrorDto error, int? statusCode) {
  final knownMessage = switch (error.code) {
    'INVALID_CREDENTIALS' => '邮箱或密码不正确。',
    'EMAIL_ALREADY_EXISTS' ||
    'ACCOUNT_ALREADY_EXISTS' ||
    'USER_ALREADY_EXISTS' =>
      '该邮箱已注册，请直接登录。',
    'VALIDATION_ERROR' => '输入内容不符合要求，请检查后重试。',
    'RATE_LIMITED' => '操作过于频繁，请稍后重试。',
    _ => null,
  };
  if (knownMessage != null) {
    return knownMessage;
  }

  final statusMessage = switch (statusCode) {
    401 => '登录状态已失效，请重新登录。',
    403 => '当前账号无权执行此操作。',
    404 => '请求的服务不存在，请检查 App 版本。',
    409 => '该账号已存在，请直接登录。',
    429 => '操作过于频繁，请稍后重试。',
    500 || 502 || 503 || 504 => '服务器暂时不可用，请稍后重试。',
    _ => null,
  };
  if (statusMessage != null) {
    return statusMessage;
  }

  final message = error.message.trim();
  final looksLikeChineseUserMessage =
      message.length <= 160 && RegExp(r'[\u4e00-\u9fff]').hasMatch(message);
  return looksLikeChineseUserMessage ? message : '操作失败，请稍后重试。';
}
