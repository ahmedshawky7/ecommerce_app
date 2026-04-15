import 'dart:developer';

import 'package:dio/dio.dart';
import 'package:ecommerce_app/core/networking/api_endpoints.dart';
import 'package:pretty_dio_logger/pretty_dio_logger.dart';

class DioHelper {
  Dio dio = Dio();

  DioHelper() {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiEndpoints.baseUrl,
        receiveDataWhenStatusError: true,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        sendTimeout: const Duration(seconds: 30),
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
        },
      ),
    );

    dio.interceptors.add(
      PrettyDioLogger(requestBody: true, responseBody: true),
    );
  }

  Future<Response> getRequest({
    required String endPoint,
    Map<String, dynamic>? query,
  }) async {
    try {
      Response response = await dio.get(endPoint, queryParameters: query);
      return response;
    } catch (e) {
      log(e.toString());
      rethrow;
    }
  }

  Future<Response> postRequest({
    required String endPoint,
    required Map<String, dynamic>? body,
  }) async {
    try {
      Response response = await dio.post(
        endPoint,
        data: body, // الـ data هي الـ Body في Dio
        options: Options(
          contentType: Headers.jsonContentType, // التأكيد أن الإرسال JSON
        ),
      );
      return response;
    } catch (e) {
      rethrow;
    }
  }

  Future<Response> putRequest({
    required String endPoint,
    required Map<String, dynamic>? body,
  }) async {
    try {
      Response response = await dio.put(
        endPoint,
        data: body, // الـ data هي الـ Body في Dio
        options: Options(
          contentType: Headers.jsonContentType, // التأكيد أن الإرسال JSON
        ),
      );
      return response;
    } catch (e) {
      log(e.toString());
      rethrow;
    }
  }
}
