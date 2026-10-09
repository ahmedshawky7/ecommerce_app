import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/user_model.dart';

class UsersRepository {
  final _dio = DioClient.instance.client;

  Future<Map<String, dynamic>> getUsers({
    int page = 0,
    int size = 20,
    String? keyword,
    String? role,
  }) async {
    try {
      final response = await _dio.get(
        ApiConstants.adminUsers,
        queryParameters: {
          'page': page,
          'size': size,
          if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
          if (role != null && role.isNotEmpty) 'role': role,
        },
      );
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<UserModel> toggleUserStatus(int userId) async {
    try {
      final response =
          await _dio.put('${ApiConstants.adminUsers}/$userId/toggle-status');
      return UserModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Failed to load users';
  }
}