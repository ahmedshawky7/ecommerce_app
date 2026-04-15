import 'dart:developer';

import 'package:ecommerce_app/features/cart/cubit/cart_state.dart';
import 'package:ecommerce_app/features/cart/repo/cart_repo.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class CartCubit extends Cubit<CartState> {
  CartCubit(this._cartRepo) : super(CartInitial());
  final CartRepo _cartRepo;
  Future<void> fetchCart() async {
    emit(CartLoading());
    final response = await _cartRepo.getUserCart();
    response.fold(
      (error) => emit(CartError(error)),
      (cart) => emit(CartSuccess(cart)),
    );
  }

  Future<void> addingToCart(Product product, int quantity) async {
    emit(AddingToCart());

    final response = await _cartRepo.addToCart(
      product: product,
      quantity: quantity,
    );

    response.fold(
      (error) {
        log("ERROR: $error");
        emit(ErrorAddingToCart(error));
      },
      (cart) {
        log("SUCCESS ADD TO CART");
        emit(SuccessAddingToCart(cart));
      },
    );
  }
}
