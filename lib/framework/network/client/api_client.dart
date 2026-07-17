import 'dart:async';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/common/api_response.dart';
import 'package:bandu_wrong_notebook/conversion/api/common/dio_failure_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/network/client/network_activity_tracker.dart';
import 'package:bandu_wrong_notebook/framework/persistence/secure_storage/token_storage.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

const defaultRequestTimeouts = ApiRequestTimeouts(
  connect: Duration(seconds: 10),
  send: Duration(seconds: 20),
  receive: Duration(seconds: 20),
);
const authRequestTimeouts = ApiRequestTimeouts(
  connect: Duration(seconds: 10),
  send: Duration(seconds: 20),
  receive: Duration(seconds: 20),
);
const uploadRequestTimeouts = ApiRequestTimeouts(
  connect: Duration(seconds: 15),
  send: Duration(seconds: 60),
  receive: Duration(seconds: 60),
);
const aiRequestTimeouts = ApiRequestTimeouts(
  connect: Duration(seconds: 15),
  send: Duration(seconds: 60),
  receive: Duration(minutes: 3),
);

final dioProvider = Provider<Dio>((ref) {
  final config = ref.watch(appConfigProvider);

  return Dio(
    BaseOptions(
      baseUrl: config.dioBaseUrl,
      connectTimeout: defaultRequestTimeouts.connect,
      receiveTimeout: defaultRequestTimeouts.receive,
      sendTimeout: defaultRequestTimeouts.send,
      responseType: ResponseType.json,
    ),
  );
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    dio: ref.watch(dioProvider),
    tokenStore: ref.watch(secureTokenStoreProvider),
    activityTracker: ref.watch(networkActivityTrackerProvider),
  );
});

class ApiRequestTimeouts {
  const ApiRequestTimeouts({
    required this.connect,
    required this.send,
    required this.receive,
  });

  final Duration connect;
  final Duration send;
  final Duration receive;
}

class ApiClient {
  ApiClient({
    required Dio dio,
    required TokenStore tokenStore,
    NetworkActivityTracker? activityTracker,
    ApiEnvelopeMapper envelopeMapper = const ApiEnvelopeMapper(),
    AuthDtoMapper authMapper = const AuthDtoMapper(),
    DioFailureMapper failureMapper = const DioFailureMapper(),
  })  : _dio = dio,
        _tokenStore = tokenStore,
        _activityTracker = activityTracker ?? NetworkActivityTracker(),
        _envelopeMapper = envelopeMapper,
        _authMapper = authMapper,
        _failureMapper = failureMapper {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _attachAuthHeader,
        onError: _handleAuthError,
      ),
    );
  }

  final Dio _dio;
  final TokenStore _tokenStore;
  final NetworkActivityTracker _activityTracker;
  final ApiEnvelopeMapper _envelopeMapper;
  final AuthDtoMapper _authMapper;
  final DioFailureMapper _failureMapper;
  Future<void>? _refreshing;

  Future<T> get<T>(
    String path, {
    Map<String, Object?>? queryParameters,
    ApiRequestTimeouts timeouts = defaultRequestTimeouts,
    int maxRetries = 2,
  }) {
    final relativePath = _requireRelativePath(path);
    return _request<T>(
      () => _dio.get<Object?>(
        relativePath,
        queryParameters: queryParameters,
        options: _options(timeouts),
      ),
      maxRetries: maxRetries,
    );
  }

  Future<T> post<T>(
    String path, {
    Object? data,
    Map<String, Object?>? queryParameters,
    Map<String, Object?>? headers,
    ApiRequestTimeouts timeouts = defaultRequestTimeouts,
    CancelToken? cancelToken,
  }) {
    final relativePath = _requireRelativePath(path);
    return _request<T>(
      () => _dio.post<Object?>(
        relativePath,
        data: data,
        queryParameters: queryParameters,
        options: _options(timeouts, headers: headers),
        cancelToken: cancelToken,
      ),
    );
  }

  Future<T> patch<T>(
    String path, {
    Object? data,
    ApiRequestTimeouts timeouts = defaultRequestTimeouts,
  }) {
    final relativePath = _requireRelativePath(path);
    return _request<T>(
      () => _dio.patch<Object?>(
        relativePath,
        data: data,
        options: _options(timeouts),
      ),
    );
  }

  Future<T> put<T>(
    String path, {
    Object? data,
    ApiRequestTimeouts timeouts = defaultRequestTimeouts,
  }) {
    final relativePath = _requireRelativePath(path);
    return _request<T>(
      () => _dio.put<Object?>(
        relativePath,
        data: data,
        options: _options(timeouts),
      ),
    );
  }

  Future<T> delete<T>(
    String path, {
    ApiRequestTimeouts timeouts = defaultRequestTimeouts,
  }) {
    final relativePath = _requireRelativePath(path);
    return _request<T>(
      () => _dio.delete<Object?>(
        relativePath,
        options: _options(timeouts),
      ),
    );
  }

  Future<void> refreshSession() async {
    await _refreshAccessToken();
  }

  Future<T> _request<T>(
    Future<Response<Object?>> Function() request, {
    int maxRetries = 0,
  }) async {
    var attempt = 0;
    while (true) {
      try {
        final response = await request();
        return _unwrap<T>(response);
      } on DioException catch (error, stackTrace) {
        if (attempt < maxRetries && _isRetryable(error)) {
          final delay = Duration(milliseconds: 250 * (1 << attempt));
          attempt += 1;
          await Future<void>.delayed(delay);
          continue;
        }
        _debugLog(error, stackTrace);
        throw _failureMapper.map(error);
      }
    }
  }

  Future<void> _attachAuthHeader(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    _activityTracker.recordRequest();
    if (options.path.endsWith('auth/refresh')) {
      handler.next(options);
      return;
    }

    final token = await _tokenStore.readAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  Future<void> _handleAuthError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final statusCode = error.response?.statusCode;
    final isRefreshCall = error.requestOptions.path.endsWith('auth/refresh');
    final isLogoutCall = error.requestOptions.path.endsWith('auth/logout');
    final isLoginCall = error.requestOptions.path.endsWith('auth/login');
    final isRegisterCall = error.requestOptions.path.endsWith('auth/register');
    final wasRetried = error.requestOptions.extra['authRetried'] == true;

    if (statusCode != 401 ||
        isRefreshCall ||
        isLogoutCall ||
        isLoginCall ||
        isRegisterCall ||
        wasRetried) {
      handler.next(error);
      return;
    }

    try {
      await _refreshAccessToken();
      final options = error.requestOptions;
      options.extra['authRetried'] = true;
      final token = await _tokenStore.readAccessToken();
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      final response = await _dio.fetch<Object?>(options);
      handler.resolve(response);
    } catch (_) {
      await _tokenStore.clear();
      handler.next(error);
    }
  }

  Future<void> _refreshAccessToken() async {
    final activeRefresh = _refreshing;
    if (activeRefresh != null) {
      return activeRefresh;
    }

    final refresh = _performRefresh();
    _refreshing = refresh;
    try {
      await refresh;
    } finally {
      if (identical(_refreshing, refresh)) {
        _refreshing = null;
      }
    }
  }

  Future<void> _performRefresh() async {
    final refreshToken = await _tokenStore.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      throw const AppFailure(
        code: 'AUTH_REFRESH_TOKEN_MISSING',
        message: '登录状态已失效，请重新登录。',
      );
    }

    final payload = await _request<Object?>(
      () => _dio.post<Object?>(
        'auth/refresh',
        data: RefreshSessionRequestDto(refreshToken: refreshToken).toJson(),
        options: _options(authRequestTimeouts),
      ),
    );
    final dto = RefreshSessionDto.fromJson(
      requireJsonObject(
        payload,
        context: 'refresh session response',
      ),
    );
    final tokens = _authMapper.refreshTokensFromDto(
      dto,
      previousRefreshToken: refreshToken,
    );

    if (tokens.accessToken.isEmpty) {
      throw const AppFailure(
        code: 'AUTH_ACCESS_TOKEN_MISSING',
        message: '服务端没有返回有效登录凭证。',
      );
    }

    await _tokenStore.save(tokens);
  }

  Options _options(
    ApiRequestTimeouts timeouts, {
    Map<String, Object?>? headers,
  }) {
    return Options(
      connectTimeout: timeouts.connect,
      sendTimeout: timeouts.send,
      receiveTimeout: timeouts.receive,
      headers: headers,
    );
  }

  bool _isRetryable(DioException error) {
    if (error.type == DioExceptionType.cancel) {
      return false;
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        (error.type == DioExceptionType.unknown &&
            error.error is SocketException)) {
      return true;
    }
    final statusCode = error.response?.statusCode;
    return statusCode != null && statusCode >= 500 && statusCode <= 504;
  }

  String _requireRelativePath(String path) {
    final normalized = path.trim();
    final uri = Uri.tryParse(normalized);
    if (normalized.isEmpty ||
        normalized.startsWith('/') ||
        uri == null ||
        uri.hasScheme ||
        uri.host.isNotEmpty) {
      throw const AppFailure(
        code: 'INVALID_API_PATH',
        message: 'App 网络配置错误，请更新 App 后重试。',
      );
    }
    return normalized;
  }

  T _unwrap<T>(Response<Object?> response) {
    return _envelopeMapper.unwrap<T>(
      response.data,
      statusCode: response.statusCode,
    );
  }

  void _debugLog(DioException error, StackTrace stackTrace) {
    if (!kDebugMode) {
      return;
    }
    final request = error.requestOptions;
    debugPrint(
      'HTTP ${request.method} ${request.uri.host}${request.uri.path} failed: '
      '${error.type.name}, status=${error.response?.statusCode ?? '-'}',
    );
    debugPrintStack(stackTrace: stackTrace);
  }
}
