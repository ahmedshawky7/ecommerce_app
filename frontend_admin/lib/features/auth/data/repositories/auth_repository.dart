import 'dart:convert';
import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/storage/secure_storage.dart';
import '../models/user_model.dart';

class AuthRepository {
  final _dio = DioClient.instance.client;

  Future<UserModel> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await _dio.post(
        ApiConstants.login,
        data: {'email': email, 'password': password},
      );
      final user = UserModel.fromJson(response.data);
      await _persistUser(user);
      return user;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<UserModel> register({
    required String username,
    required String email,
    required String password,
  }) async {
    try {
      final response = await _dio.post(
        ApiConstants.register,
        data: {'username': username, 'email': email, 'password': password},
      );
      final user = UserModel.fromJson(response.data);
      await _persistUser(user);
      return user;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<UserModel?> getCurrentUser() async {
    final json = await SecureStorage.instance.getUserData();
    if (json == null || json.isEmpty) return null;
    try {
      return UserModel.fromJson(jsonDecode(json) as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }

  Future<void> logout() async => await SecureStorage.instance.clearAll();

  Future<bool> isLoggedIn() async {
    final token = await SecureStorage.instance.getToken();
    return token != null && token.isNotEmpty;
  }

  Future<void> _persistUser(UserModel user) async {
    await SecureStorage.instance.saveToken(user.token);
    await SecureStorage.instance.saveRefreshToken(user.refreshToken);
    await SecureStorage.instance.saveUserData(jsonEncode(user.toJson()));
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    if (e.type == DioExceptionType.connectionError) {
      return 'Cannot connect to server';
    }
    return 'Something went wrong. Please try again.';
  }
}