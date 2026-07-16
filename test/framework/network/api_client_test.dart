import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('GET keeps the Mobile API base path and retries twice', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    var requests = 0;
    final paths = <String>[];
    server.listen((request) {
      requests += 1;
      paths.add(request.uri.path);
      request.response.headers.contentType = ContentType.json;
      if (requests < 3) {
        request.response.statusCode = HttpStatus.serviceUnavailable;
        request.response.write(
          jsonEncode({
            'error': {'code': 'UNAVAILABLE', 'message': 'temporary'},
          }),
        );
      } else {
        request.response.write(
          jsonEncode({
            'data': {'ok': true},
          }),
        );
      }
      unawaited(request.response.close());
    });
    final dio = Dio(
      BaseOptions(baseUrl: '${_origin(server)}/api/mobile/v1/'),
    );
    addTearDown(() => dio.close(force: true));
    final client = ApiClient(dio: dio, tokenStore: _MemoryTokenStore());

    final payload = await client.get<Object?>('error-items');

    expect((payload as Map<String, Object?>)['ok'], isTrue);
    expect(requests, 3);
    expect(paths, everyElement('/api/mobile/v1/error-items'));
  });

  test('POST does not retry failed writes', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    var requests = 0;
    server.listen((request) {
      requests += 1;
      request.response
        ..statusCode = HttpStatus.serviceUnavailable
        ..headers.contentType = ContentType.json
        ..write(
          jsonEncode({
            'error': {'code': 'UNAVAILABLE', 'message': 'temporary'},
          }),
        );
      unawaited(request.response.close());
    });
    final dio = Dio(
      BaseOptions(baseUrl: '${_origin(server)}/api/mobile/v1/'),
    );
    addTearDown(() => dio.close(force: true));
    final client = ApiClient(dio: dio, tokenStore: _MemoryTokenStore());

    await expectLater(
      client.post<Object?>('error-items', data: {'title': '题目'}),
      throwsA(
        isA<AppFailure>()
            .having((failure) => failure.statusCode, 'statusCode', 503),
      ),
    );
    expect(requests, 1);
  });

  test('absolute and leading-slash paths are rejected before transport', () {
    final dio = Dio(
      BaseOptions(baseUrl: 'https://aibandu.dpdns.org/api/mobile/v1/'),
    );
    addTearDown(() => dio.close(force: true));
    final client = ApiClient(dio: dio, tokenStore: _MemoryTokenStore());

    expect(
      () => client.get<Object?>('/error-items'),
      throwsA(
        isA<AppFailure>().having(
          (failure) => failure.code,
          'code',
          'INVALID_API_PATH',
        ),
      ),
    );
    expect(
      () => client.get<Object?>('https://example.com/error-items'),
      throwsA(isA<AppFailure>()),
    );
  });

  test('concurrent 401 responses share one refresh request', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    var refreshRequests = 0;
    var protectedRequests = 0;
    server.listen((request) async {
      request.response.headers.contentType = ContentType.json;
      if (request.uri.path.endsWith('/auth/refresh')) {
        refreshRequests += 1;
        await Future<void>.delayed(const Duration(milliseconds: 100));
        request.response.write(
          jsonEncode({
            'data': {
              'accessToken': 'new-access',
              'refreshToken': 'new-refresh',
            },
          }),
        );
      } else {
        protectedRequests += 1;
        if (request.headers.value(HttpHeaders.authorizationHeader) ==
            'Bearer new-access') {
          request.response.write(jsonEncode({'data': 'ok'}));
        } else {
          request.response.statusCode = HttpStatus.unauthorized;
          request.response.write(
            jsonEncode({
              'error': {'code': 'UNAUTHORIZED', 'message': 'expired'},
            }),
          );
        }
      }
      await request.response.close();
    });
    final dio = Dio(
      BaseOptions(baseUrl: '${_origin(server)}/api/mobile/v1/'),
    );
    addTearDown(() => dio.close(force: true));
    final tokens = _MemoryTokenStore()
      .._tokens = const TokenPair(
        accessToken: 'expired-access',
        refreshToken: 'refresh-token',
      );
    final client = ApiClient(dio: dio, tokenStore: tokens);

    final values = await Future.wait([
      client.get<String>('protected/one'),
      client.get<String>('protected/two'),
    ]);

    expect(values, ['ok', 'ok']);
    expect(refreshRequests, 1);
    expect(protectedRequests, 4);
    expect(await tokens.readAccessToken(), 'new-access');
    expect(await tokens.readRefreshToken(), 'new-refresh');
  });

  test('login 401 does not attempt token refresh', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    var refreshRequests = 0;
    server.listen((request) {
      if (request.uri.path.endsWith('/auth/refresh')) {
        refreshRequests += 1;
      }
      request.response
        ..statusCode = HttpStatus.unauthorized
        ..headers.contentType = ContentType.json
        ..write(
          jsonEncode({
            'error': {
              'code': 'INVALID_CREDENTIALS',
              'message': 'invalid',
            },
          }),
        );
      unawaited(request.response.close());
    });
    final dio = Dio(
      BaseOptions(baseUrl: '${_origin(server)}/api/mobile/v1/'),
    );
    addTearDown(() => dio.close(force: true));
    final tokens = _MemoryTokenStore()
      .._tokens = const TokenPair(
        accessToken: 'old-access',
        refreshToken: 'old-refresh',
      );
    final client = ApiClient(dio: dio, tokenStore: tokens);

    await expectLater(
      client.post<Object?>('auth/login', data: {'email': 'a@b.com'}),
      throwsA(
        isA<AppFailure>().having(
          (failure) => failure.code,
          'code',
          'INVALID_CREDENTIALS',
        ),
      ),
    );
    expect(refreshRequests, 0);
  });
}

String _origin(HttpServer server) {
  return 'http://${server.address.address}:${server.port}';
}

class _MemoryTokenStore implements TokenStore {
  TokenPair? _tokens;

  @override
  Future<void> clear() async {
    _tokens = null;
  }

  @override
  Future<String?> readAccessToken() async => _tokens?.accessToken;

  @override
  Future<String?> readRefreshToken() async => _tokens?.refreshToken;

  @override
  Future<void> save(TokenPair tokenPair) async {
    _tokens = tokenPair;
  }
}
