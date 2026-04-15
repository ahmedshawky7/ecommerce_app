import 'package:ecommerce_app/features/cart/models/cart_model.dart';

abstract class CartState {}

final class CartInitial extends CartState {}

final class CartLoading extends CartState {}

final class CartSuccess extends CartState {
  final CartModel cartModel;
  CartSuccess(this.cartModel);
}

final class CartError extends CartState {
  final String error;
  CartError(this.error);
}

class AddingToCart extends CartState {}

class SuccessAddingToCart extends CartState {
  final CartModel cartModel;
  SuccessAddingToCart(this.cartModel);
}

class ErrorAddingToCart extends CartState {
  final String error;
  ErrorAddingToCart(this.error);
}
