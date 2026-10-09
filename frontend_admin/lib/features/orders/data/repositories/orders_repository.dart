import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/order_model.dart';

class OrdersRepository {
  final _dio = DioClient.instance.client;

  Future<Map<String, dynamic>> getOrders({
    int page = 0,
    int size = 20,
    String? status,
    String? keyword,
  }) async {
    try {
      final response = await _dio.get(
        ApiConstants.adminOrders,
        queryParameters: {
          'page': page,
          'size': size,
          if (status != null) 'status': status,
          if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        },
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<OrderModel> getOrderById(int id) async {
    try {
      final response = await _dio.get('${ApiConstants.adminOrders}/$id');
      return OrderModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Failed to load orders';
  }
}