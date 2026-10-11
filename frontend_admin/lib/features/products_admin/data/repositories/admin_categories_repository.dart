import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../products/data/models/category_model.dart';

class AdminCategoriesRepository {
  final _dio = DioClient.instance.client;

  /// Get all categories
  Future<List<CategoryModel>> getCategories() async {
    try {
      final response = await _dio.get(ApiConstants.categories);
      return (response.data as List)
          .map((e) => CategoryModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Create new category
  Future<CategoryModel> createCategory({
    required String name,
    String? description,
    String? imageUrl,
  }) async {
    try {
      final response = await _dio.post(
        ApiConstants.categories,
        data: {
          'name': name,
          if (description != null && description.isNotEmpty)
            'description': description,
          if (imageUrl != null && imageUrl.isNotEmpty) 'imageUrl': imageUrl,
        },
      );
      return CategoryModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Update existing category
  Future<CategoryModel> updateCategory({
    required int id,
    required String name,
    String? description,
    String? imageUrl,
  }) async {
    try {
      final response = await _dio.put(
        '${ApiConstants.categories}/$id',
        data: {
          'name': name,
          if (description != null && description.isNotEmpty)
            'description': description,
          if (imageUrl != null && imageUrl.isNotEmpty) 'imageUrl': imageUrl,
        },
      );
      return CategoryModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  /// Delete category
  Future<void> deleteCategory(int id) async {
    try {
      await _dio.delete('${ApiConstants.categories}/$id');
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Category operation failed';
  }
}