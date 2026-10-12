import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../constants/api_constants.dart';
import '../storage/secure_storage.dart';

class DioClient {
  DioClient._();
  static final DioClient instance = DioClient._();

  late final Dio dio;
  bool _initialized = false;
  bool _isRefreshing = false;

  Dio get client {
    if (!_initialized) _init();
    return dio;
  }

  void _init() {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiConstants.baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await SecureStorage.instance.getToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          if (kDebugMode) {
            debugPrint('🚀 [${options.method}] ${options.uri}');
          }
          return handler.next(options);
        },
        onResponse: (response, handler) {
          if (kDebugMode) {
            debugPrint('✅ [${response.statusCode}] ${response.requestOptions.uri}');
          }
          return handler.next(response);
        },
        onError: (error, handler) async {
          if (kDebugMode) {
            debugPrint('❌ [${error.response?.statusCode}] ${error.requestOptions.uri}');
          }

          // ⚡ Handle 401: Try to refresh token
          if (error.response?.statusCode == 401 && !_isRefreshing) {
            final refreshToken = await SecureStorage.instance.getRefreshToken();

            if (refreshToken != null && refreshToken.isNotEmpty) {
              _isRefreshing = true;
              try {
                // Call refresh endpoint using a clean Dio (no interceptor)
                final refreshDio = Dio(BaseOptions(baseUrl: ApiConstants.baseUrl));
                final refreshResponse = await refreshDio.post(
                  ApiConstants.refresh,
                  data: {'refreshToken': refreshToken},
                );

                final newToken = refreshResponse.data['token'] as String?;
                final newRefreshToken = refreshResponse.data['refreshToken'] as String?;

                if (newToken != null) {
                  await SecureStorage.instance.saveToken(newToken);
                  if (newRefreshToken != null) {
                    await SecureStorage.instance.saveRefreshToken(newRefreshToken);
                  }

                  // Retry the original request with new token
                  final opts = error.requestOptions;
                  opts.headers['Authorization'] = 'Bearer $newToken';

                  final clonedResponse = await dio.fetch(opts);
                  _isRefreshing = false;
                  return handler.resolve(clonedResponse);
                }
              } catch (e) {
                if (kDebugMode) debugPrint('🔄 Refresh failed: $e');
                _isRefreshing = false;
              }
            }
          }

          return handler.next(error);
        },
      ),
    );

    _initialized = true;
  }
}