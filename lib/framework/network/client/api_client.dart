import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/auth/domain/token_store.dart';
import 'package:bandu_wrong_notebook/conversion/api/auth/auth_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/api/common/api_response.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/persistence/secure_storage/token_storage.dart';
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
    tokenStore: ref.watch(secureTokenStoreProvider),
  );
});

class ApiClient {
  ApiClient({
    required Dio dio,
    required TokenStore tokenStore,
    ApiEnvelopeMapper envelopeMapper = const ApiEnvelopeMapper(),
    AuthDtoMapper authMapper = const AuthDtoMapper(),
  })  : _dio = dio,
        _tokenStore = tokenStore,
        _envelopeMapper = envelopeMapper,
        _authMapper = authMapper {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _attachAuthHeader,
        onError: _handleAuthError,
      ),
    );
  }

  final Dio _dio;
  final TokenStore _tokenStore;
  final ApiEnvelopeMapper _envelopeMapper;
  final AuthDtoMapper _authMapper;
  Future<void>? _refreshing;

  Future<T> get<T>(String path, {Map<String, Object?>? queryParameters}) async {
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

  Future<T> patch<T>(String path, {Object? data}) async {
    final response = await _dio.patch<Object?>(path, data: data);
    return _unwrap<T>(response);
  }

  Future<T> delete<T>(String path) async {
    final response = await _dio.delete<Object?>(path);
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
    final wasRetried = error.requestOptions.extra['authRetried'] == true;

    if (statusCode != 401 || isRefreshCall || wasRetried) {
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
        message: '登录状态已失效',
      );
    }

    final response = await _dio.post<Object?>(
      '/auth/refresh',
      data: RefreshSessionRequestDto(refreshToken: refreshToken).toJson(),
    );
    final dto = RefreshSessionDto.fromJson(
      requireJsonObject(
        _unwrap<Object?>(response),
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
        message: '服务端没有返回有效登录凭证',
      );
    }

    await _tokenStore.save(tokens);
  }

  T _unwrap<T>(Response<Object?> response) {
    return _envelopeMapper.unwrap<T>(
      response.data,
      statusCode: response.statusCode,
    );
  }
}
