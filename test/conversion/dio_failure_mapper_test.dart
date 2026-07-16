import 'dart:io';

import 'package:bandu_wrong_notebook/conversion/api/common/dio_failure_mapper.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const mapper = DioFailureMapper();

  test('maps DNS lookup failure to a Chinese user message', () {
    final failure = mapper.map(
      DioException(
        requestOptions: RequestOptions(path: '/health'),
        type: DioExceptionType.connectionError,
        error: const SocketException('Failed host lookup: example.invalid'),
      ),
    );

    expect(failure.code, 'DNS_LOOKUP_FAILED');
    expect(failure.userMessage, '无法解析服务地址，请稍后重试。');
    expect(failure.userMessage, isNot(contains('SocketException')));
  });

  test('maps timeout and cancellation without exposing Dio details', () {
    final timeout = mapper.map(
      DioException(
        requestOptions: RequestOptions(path: '/items'),
        type: DioExceptionType.receiveTimeout,
      ),
    );
    final cancelled = mapper.map(
      DioException(
        requestOptions: RequestOptions(path: '/items'),
        type: DioExceptionType.cancel,
      ),
    );

    expect(timeout.userMessage, '服务器处理时间较长，请稍后重试。');
    expect(cancelled.isCancellation, isTrue);
  });

  test('maps server HTML response to stable status message', () {
    final request = RequestOptions(path: '/items');
    final failure = mapper.map(
      DioException.badResponse(
        statusCode: 503,
        requestOptions: request,
        response: Response<Object?>(
          requestOptions: request,
          statusCode: 503,
          data: '<html>cloud edge failure</html>',
        ),
      ),
    );

    expect(failure.statusCode, 503);
    expect(failure.userMessage, '服务器暂时不可用，请稍后重试。');
    expect(failure.userMessage, isNot(contains('html')));
  });

  test('uses known business error without exposing raw server message', () {
    final request = RequestOptions(path: '/auth/register');
    final failure = mapper.map(
      DioException.badResponse(
        statusCode: 409,
        requestOptions: request,
        response: Response<Object?>(
          requestOptions: request,
          statusCode: 409,
          data: {
            'error': {
              'code': 'EMAIL_ALREADY_EXISTS',
              'message': 'internal English details',
            },
          },
        ),
      ),
    );

    expect(failure.code, 'EMAIL_ALREADY_EXISTS');
    expect(failure.userMessage, '该邮箱已注册，请直接登录。');
  });
}
