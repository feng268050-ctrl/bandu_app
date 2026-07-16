import 'dart:async';
import 'dart:io';

import 'package:bandu_wrong_notebook/framework/network/client/network_activity_tracker.dart';
import 'package:bandu_wrong_notebook/framework/network/services/health_api_service.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('health check uses direct health URL and retries one transient failure',
      () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    addTearDown(() => server.close(force: true));
    var requests = 0;
    final paths = <String>[];
    server.listen((request) {
      requests += 1;
      paths.add(request.uri.path);
      request.response.statusCode =
          requests == 1 ? HttpStatus.serviceUnavailable : HttpStatus.noContent;
      unawaited(request.response.close());
    });
    final dio = Dio();
    addTearDown(() => dio.close(force: true));
    final tracker = NetworkActivityTracker();
    final service = HealthApiService(
      dio: dio,
      healthCheckUri: Uri.parse(
        'http://${server.address.address}:${server.port}/api/health',
      ),
      activityTracker: tracker,
    );

    final statusCode = await service.check();

    expect(statusCode, HttpStatus.noContent);
    expect(requests, 2);
    expect(paths, ['/api/health', '/api/health']);
    expect(tracker.lastRequestAt, isNotNull);
  });
}
