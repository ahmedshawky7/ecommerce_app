import 'cart_item_model.dart';

class CartModel {
  final int id;
  final List<CartItemModel> items;
  final int totalItems;
  final double totalPrice;

  CartModel({
    required this.id,
    required this.items,
    required this.totalItems,
    required this.totalPrice,
  });

  factory CartModel.fromJson(Map<String, dynamic> json) {
    return CartModel(
      id: json['id'] as int,
      items: (json['items'] as List)
          .map((e) => CartItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalItems: json['totalItems'] as int,
      totalPrice: (json['totalPrice'] as num).toDouble(),
    );
  }

  static CartModel empty() => CartModel(
        id: 0,
        items: [],
        totalItems: 0,
        totalPrice: 0.0,
      );
}