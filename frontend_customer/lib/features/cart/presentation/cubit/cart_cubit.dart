import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/cart_model.dart';
import '../../data/repositories/cart_repository.dart';

part 'cart_state.dart';

class CartCubit extends Cubit<CartState> {
  final CartRepository _repository;
  CartCubit(this._repository) : super(CartInitial());

  Future<void> loadCart() async {
    emit(CartLoading());
    try {
      final cart = await _repository.getCart();
      emit(CartLoaded(cart));
    } catch (e) {
      emit(CartError(e.toString()));
    }
  }

  Future<void> addToCart({
    required int productId,
    required int quantity,
  }) async {
    try {
      final cart = await _repository.addToCart(
        productId: productId,
        quantity: quantity,
      );
      emit(CartLoaded(cart));
    } catch (e) {
      emit(CartError(e.toString()));
    }
  }

  Future<void> updateQuantity({
    required int itemId,
    required int quantity,
  }) async {
    try {
      final cart = await _repository.updateCartItem(
        itemId: itemId,
        quantity: quantity,
      );
      emit(CartLoaded(cart));
    } catch (e) {
      emit(CartError(e.toString()));
    }
  }

  Future<void> removeItem(int itemId) async {
    try {
      await _repository.removeCartItem(itemId);
      await loadCart();
    } catch (e) {
      emit(CartError(e.toString()));
    }
  }

  Future<void> clearCart() async {
    try {
      await _repository.clearCart();
      emit(CartLoaded(CartModel.empty()));
    } catch (e) {
      emit(CartError(e.toString()));
    }
  }
}