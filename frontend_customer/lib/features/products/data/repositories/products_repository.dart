import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/product_model.dart';

class ProductsRepository {
  final _dio = DioClient.instance.client;

  Future<Map<String, dynamic>> getProducts({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _dio.get(
        ApiConstants.products,
        queryParameters: {'page': page, 'size': size},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> getProductsByCategory({
    required int categoryId,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _dio.get(
        '${ApiConstants.products}/category/$categoryId',
        queryParameters: {'page': page, 'size': size},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> searchProducts({
    required String keyword,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _dio.get(
        '${ApiConstants.products}/search',
        queryParameters: {'keyword': keyword, 'page': page, 'size': size},
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<ProductModel> getProductById(int id) async {
    try {
      final response = await _dio.get('${ApiConstants.products}/$id');
      return ProductModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Failed to load products';
  }
}