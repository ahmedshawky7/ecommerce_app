import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/sales_analytics_model.dart';

class AnalyticsRepository {
  final _dio = DioClient.instance.client;

  Future<List<SalesAnalyticsModel>> getDailySales({int days = 7}) async {
    try {
      final response = await _dio.get(
        ApiConstants.salesAnalytics,
        queryParameters: {'days': days},
      );
      return (response.data as List)
          .map((e) => SalesAnalyticsModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<RevenueByCategoryModel>> getRevenueByCategory() async {
    try {
      final response = await _dio.get(ApiConstants.revenueByCategory);
      return (response.data as List)
          .map((e) => RevenueByCategoryModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Failed to load analytics';
  }
}