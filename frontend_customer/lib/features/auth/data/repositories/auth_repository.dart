import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/storage/secure_storage.dart';
import '../models/user_model.dart';

class AuthRepository {
  final _dio = DioClient.instance.client;

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
      await SecureStorage.instance.saveToken(user.token);
      await SecureStorage.instance.saveRefreshToken(user.refreshToken);
      return user;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

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
      await SecureStorage.instance.saveToken(user.token);
      await SecureStorage.instance.saveRefreshToken(user.refreshToken);
      return user;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> logout() async => await SecureStorage.instance.clearAll();

  Future<bool> isLoggedIn() async {
    final token = await SecureStorage.instance.getToken();
    return token != null && token.isNotEmpty;
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    if (e.type == DioExceptionType.connectionError) {
      return 'Cannot connect to server. Is backend running?';
    }
    return 'Something went wrong. Please try again.';
  }
}