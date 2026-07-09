import 'package:bandu_wrong_notebook/core/config/app_config.dart';
import 'package:bandu_wrong_notebook/core/network/api_exception.dart';
import 'package:bandu_wrong_notebook/core/network/api_response.dart';
import 'package:bandu_wrong_notebook/core/storage/token_storage.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final dioProvider = Provider<Dio>((ref) {
  final config = ref.watch(appConfigProvider);

  return Dio(
    BaseOptions(
      baseUrl: config.apiBaseUrl,
      connectTimeout: const Duration(seconds: 20),
      receiveTimeout: const Duration(seconds: 60),
      sendTimeout: const Duration(seconds: 60),
      responseType: ResponseType.json,
    ),
  );
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    dio: ref.watch(dioProvider),
    tokenStore: ref.watch(tokenStoreProvider),
  );
});

class ApiClient {
  ApiClient({
    required Dio dio,
    required TokenStore tokenStore,
  })  : _dio = dio,
        _tokenStore = tokenStore {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _attachAuthHeader,
        onError: _handleAuthError,
      ),
    );
  }

  final Dio _dio;
  final TokenStore _tokenStore;

  Future<T> get<T>(
    String path, {
    Map<String, Object?>? queryParameters,
  }) async {
    final response = await _dio.get<Object?>(
      path,
      queryParameters: queryParameters,
    );
    return _unwrap<T>(response);
  }

  Future<T> post<T>(
    String path, {
    Object? data,
    Map<String, Object?>? queryParameters,
  }) async {
    final response = await _dio.post<Object?>(
      path,
      data: data,
      queryParameters: queryParameters,
    );
    return _unwrap<T>(response);
  }

  Future<void> refreshSession() async {
    await _refreshAccessToken();
  }

  Future<void> _attachAuthHeader(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (options.path.endsWith('/auth/refresh')) {
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
    final isRefreshCall = error.requestOptions.path.endsWith('/auth/refresh');

    if (statusCode != 401 || isRefreshCall) {
      handler.next(error);
      return;
    }

    try {
      await _refreshAccessToken();
      final options = error.requestOptions;
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
    final refreshToken = await _tokenStore.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      throw const ApiException(
        code: 'AUTH_REFRESH_TOKEN_MISSING',
        message: '登录状态已失效',
      );
    }

    final response = await _dio.post<Object?>(
      '/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    final data = _unwrap<Map<String, Object?>>(response);
    final accessToken = data['accessToken']?.toString();
    final nextRefreshToken = data['refreshToken']?.toString() ?? refreshToken;

    if (accessToken == null || accessToken.isEmpty) {
      throw const ApiException(
        code: 'AUTH_ACCESS_TOKEN_MISSING',
        message: '服务端没有返回有效登录凭证',
      );
    }

    await _tokenStore.save(
      TokenPair(
        accessToken: accessToken,
        refreshToken: nextRefreshToken,
      ),
    );
  }

  T _unwrap<T>(Response<Object?> response) {
    final payload = response.data;

    if (payload is Map<String, Object?>) {
      final errorPayload = payload['error'];
      if (errorPayload is Map<String, Object?>) {
        final apiError = ApiError.fromJson(errorPayload);
        throw ApiException(
          code: apiError.code,
          message: apiError.message,
          statusCode: response.statusCode,
          details: apiError.details,
        );
      }

      if (payload.containsKey('data')) {
        return payload['data'] as T;
      }
    }

    return payload as T;
  }
}
