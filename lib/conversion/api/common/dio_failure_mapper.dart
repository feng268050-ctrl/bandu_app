import 'dart:io';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/conversion/api/common/api_response.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:dio/dio.dart';

class DioFailureMapper {
  const DioFailureMapper();

  AppFailure map(DioException error) {
    final businessFailure = _businessFailure(error.response);
    if (businessFailure != null) {
      return businessFailure;
    }

    return switch (error.type) {
      DioExceptionType.cancel => _failure(
          'REQUEST_CANCELLED',
          '操作已取消。',
          error,
        ),
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout =>
        _failure(
          'CONNECTION_TIMEOUT',
          '连接服务器超时，请检查网络后重试。',
          error,
        ),
      DioExceptionType.receiveTimeout => _failure(
          'RECEIVE_TIMEOUT',
          '服务器处理时间较长，请稍后重试。',
          error,
        ),
      DioExceptionType.transformTimeout => _failure(
          'RESPONSE_PROCESSING_TIMEOUT',
          '响应数据处理超时，请稍后重试。',
          error,
        ),
      DioExceptionType.badCertificate => _failure(
          'TLS_CERTIFICATE_ERROR',
          '无法建立安全连接，请检查系统时间或稍后重试。',
          error,
        ),
      DioExceptionType.badResponse => _statusFailure(error),
      DioExceptionType.connectionError ||
      DioExceptionType.unknown =>
        _connectionFailure(error),
    };
  }

  AppFailure _connectionFailure(DioException error) {
    final cause = error.error;
    final technical = cause.toString().toLowerCase();
    final isDnsFailure = cause is SocketException &&
        (technical.contains('failed host lookup') ||
            technical.contains('nodename nor servname') ||
            technical.contains('name or service not known'));

    return _failure(
      isDnsFailure ? 'DNS_LOOKUP_FAILED' : 'NETWORK_UNAVAILABLE',
      isDnsFailure ? '无法解析服务地址，请稍后重试。' : '当前网络不可用，请检查 Wi-Fi 或移动数据。',
      error,
    );
  }

  AppFailure _statusFailure(DioException error) {
    final statusCode = error.response?.statusCode;
    final message = switch (statusCode) {
      400 => '请求内容有误，请检查后重试。',
      401 => '登录状态已失效，请重新登录。',
      403 => '当前账号无权执行此操作。',
      404 => '请求的服务不存在，请检查 App 版本。',
      409 => '该账号已存在，请直接登录。',
      429 => '操作过于频繁，请稍后重试。',
      500 || 502 || 503 || 504 => '服务器暂时不可用，请稍后重试。',
      _ => '操作失败，请稍后重试。',
    };
    return AppFailure(
      code: 'HTTP_${statusCode ?? 'UNKNOWN'}',
      message: message,
      statusCode: statusCode,
      details: _technicalDetails(error),
    );
  }

  AppFailure? _businessFailure(Response<Object?>? response) {
    final payload = response?.data;
    if (payload is! Map) {
      return null;
    }

    try {
      final envelope =
          requireJsonObject(payload, context: 'API error envelope');
      final rawError = envelope['error'];
      if (rawError is! Map) {
        return null;
      }
      final apiError = ApiErrorDto.fromJson(
        requireJsonObject(rawError, context: 'API error'),
      );
      final message = switch (apiError.code) {
        'INVALID_CREDENTIALS' => '邮箱或密码不正确。',
        'EMAIL_ALREADY_EXISTS' ||
        'ACCOUNT_ALREADY_EXISTS' ||
        'USER_ALREADY_EXISTS' =>
          '该邮箱已注册，请直接登录。',
        'VALIDATION_ERROR' => '输入内容不符合要求，请检查后重试。',
        'RATE_LIMITED' => '操作过于频繁，请稍后重试。',
        _ => null,
      };
      if (message == null) {
        return null;
      }
      return AppFailure(
        code: apiError.code,
        message: message,
        statusCode: response?.statusCode,
        details: apiError.details,
      );
    } catch (_) {
      return null;
    }
  }

  AppFailure _failure(
    String code,
    String message,
    DioException error,
  ) {
    return AppFailure(
      code: code,
      message: message,
      statusCode: error.response?.statusCode,
      details: _technicalDetails(error),
    );
  }

  String _technicalDetails(DioException error) {
    return '${error.type.name}: ${error.error ?? error.message ?? 'unknown'}';
  }
}
