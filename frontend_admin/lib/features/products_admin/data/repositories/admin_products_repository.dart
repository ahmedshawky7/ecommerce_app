import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../products/data/models/product_model.dart';

class AdminProductsRepository {
  final _dio = DioClient.instance.client;

  /// Get products (paginated)
  Future<Map<String, dynamic>> getProducts({
    int page = 0,
    int size = 20,
    String? keyword,
  }) async {
    try {
      final response = await _dio.get(
        ApiConstants.products,
        queryParameters: {
          'page': page,
          'size': size,
          if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        },
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Create new product
  Future<ProductModel> createProduct({
    required String name,
    required String description,
    required double price,
    required int stockQuantity,
    String? imageUrl,
    required int categoryId,
  }) async {
    try {
      final response = await _dio.post(
        ApiConstants.products,
        data: {
          'name': name,
          'description': description,
          'price': price,
          'stockQuantity': stockQuantity,
          if (imageUrl != null && imageUrl.isNotEmpty) 'imageUrl': imageUrl,
          'categoryId': categoryId,
        },
      );
      return ProductModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Update existing product
  Future<ProductModel> updateProduct({
    required int id,
    required String name,
    required String description,
    required double price,
    required int stockQuantity,
    String? imageUrl,
    required int categoryId,
  }) async {
    try {
      final response = await _dio.put(
        '${ApiConstants.products}/$id',
        data: {
          'name': name,
          'description': description,
          'price': price,
          'stockQuantity': stockQuantity,
          if (imageUrl != null && imageUrl.isNotEmpty) 'imageUrl': imageUrl,
          'categoryId': categoryId,
        },
      );
      return ProductModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Delete product (soft delete)
  Future<void> deleteProduct(int id) async {
    try {
      await _dio.delete('${ApiConstants.products}/$id');
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Product operation failed';
  }
}