import 'package:dartz/dartz.dart';
import 'package:dio/dio.dart';
import 'package:ecommerce_app/core/networking/api_endpoints.dart';
import 'package:ecommerce_app/core/networking/dio_helper.dart';
import 'package:ecommerce_app/core/utils/service_locator.dart';
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:ecommerce_app/features/auth/models/login_response_model.dart';

class AuthRepo {
  final DioHelper _dioHelper;

  AuthRepo(this._dioHelper);
  Future<Either<String, LoginResponseModel>> login(
    String username,
    String password,
  ) async {
    try {
      final response = await _dioHelper.postRequest(
        endPoint: ApiEndpoints.login,
        body: {'username': username, 'password': password},
      );
      //emilys
      if (response.statusCode == 200 || response.statusCode == 201) {
        LoginResponseModel loginResponseModel = LoginResponseModel.fromJson(
          response.data,
        );
        if (loginResponseModel.accessToken != null) {
          await sl<StorageHelper>().saveToken(value: loginResponseModel.accessToken!);
          return Right(loginResponseModel);
        } else {
          return Left('Token is null in Model, check accessToken field');
        }
      } else {
        return Left(response.toString());
      }
    } catch (e) {
      if (e is DioException) {
        return Left(e.response.toString());
      }
      return Left(e.toString());
    }
  }
  
}
