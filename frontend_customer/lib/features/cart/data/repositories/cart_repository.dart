import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../models/cart_model.dart';

class CartRepository {
  final _dio = DioClient.instance.client;

  Future<CartModel> getCart() async {
    try {
      final response = await _dio.get(ApiConstants.cart);
      return CartModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<CartModel> addToCart({
    required int productId,
    required int quantity,
  }) async {
    try {
      final response = await _dio.post(
        ApiConstants.cartItems,
        data: {'productId': productId, 'quantity': quantity},
      );
      return CartModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<CartModel> updateCartItem({
    required int itemId,
    required int quantity,
  }) async {
    try {
      final response = await _dio.put(
        '${ApiConstants.cartItems}/$itemId',
        data: {'quantity': quantity},
      );
      return CartModel.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> removeCartItem(int itemId) async {
    try {
      await _dio.delete('${ApiConstants.cartItems}/$itemId');
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> clearCart() async {
    try {
      await _dio.delete(ApiConstants.cart);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    return 'Cart operation failed';
  }
}