import 'dart:async';

import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/network/client/network_activity_tracker.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final healthApiServiceProvider = Provider<HealthApiService>((ref) {
  final config = ref.watch(appConfigProvider);
  return HealthApiService(
    dio: Dio(
      BaseOptions(
        connectTimeout: const Duration(seconds: 5),
        sendTimeout: const Duration(seconds: 5),
        receiveTimeout: const Duration(seconds: 5),
        responseType: ResponseType.json,
      ),
    ),
    healthCheckUri: config.healthCheckUri,
    activityTracker: ref.watch(networkActivityTrackerProvider),
  );
});

class HealthApiService {
  const HealthApiService({
    required Dio dio,
    required Uri healthCheckUri,
    required NetworkActivityTracker activityTracker,
  })  : _dio = dio,
        _healthCheckUri = healthCheckUri,
        _activityTracker = activityTracker;

  final Dio _dio;
  final Uri _healthCheckUri;
  final NetworkActivityTracker _activityTracker;

  Future<int?> check() async {
    DioException? lastError;
    for (var attempt = 0; attempt < 2; attempt += 1) {
      try {
        _activityTracker.recordRequest();
        final response = await _dio.getUri<Object?>(_healthCheckUri);
        return response.statusCode;
      } on DioException catch (error) {
        lastError = error;
        if (attempt == 0 && _isRetryable(error)) {
          await Future<void>.delayed(const Duration(milliseconds: 250));
          continue;
        }
        rethrow;
      }
    }
    throw lastError!;
  }

  bool _isRetryable(DioException error) {
    final statusCode = error.response?.statusCode;
    return error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        (statusCode != null && statusCode >= 500 && statusCode <= 504);
  }
}
