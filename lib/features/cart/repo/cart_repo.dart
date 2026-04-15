import 'package:dartz/dartz.dart';
import 'package:ecommerce_app/core/networking/api_endpoints.dart';
import 'package:ecommerce_app/core/networking/dio_helper.dart';
import 'package:ecommerce_app/features/cart/models/cart_model.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';

class CartRepo {
  final DioHelper _dioHelper;

  CartRepo(this._dioHelper);

  Future<Either<String, CartModel>> getUserCart() async {
    try {
      final response = await _dioHelper.getRequest(
        endPoint: ApiEndpoints.cart + "/1",
      );
      if (response.statusCode == 200) {
        CartModel cartModel = CartModel.fromJson(response.data);
        return Right(cartModel);
      } else {
        return Left("Something went wrong In Cart Repo");
      }
    } catch (e) {
      return Left(e.toString());
    }
  }

  Future<Either<String, CartModel>> addToCart({
    required Product product,
    required int quantity,
  }) async {
    try {
      final response = await _dioHelper.putRequest(
        endPoint: ApiEndpoints.cart + "/1",
        body: {
          'userId': 1,
          'products': [
            {'id': product.id, 'quantity': quantity},
          ],
        },
      );
      if (response.statusCode == 200) {
        CartModel cartModel = CartModel.fromJson(response.data);
        return Right(cartModel);
      } else {
        return Left("Something went wrong In Cart Repo");
      }
    } catch (e) {
      return Left(e.toString());
    }
  }
}
