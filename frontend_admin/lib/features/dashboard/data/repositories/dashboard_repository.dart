import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/dashboard_stats_model.dart';

class DashboardRepository {
  final _dio = DioClient.instance.client;

  Future<DashboardStatsModel> getStats() async {
    try {
      final response = await _dio.get(ApiConstants.dashboardStats);
      return DashboardStatsModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    if (e.type == DioExceptionType.connectionError) {
      return 'Cannot connect to server';
    }
    return 'Failed to load dashboard stats';
  }
}