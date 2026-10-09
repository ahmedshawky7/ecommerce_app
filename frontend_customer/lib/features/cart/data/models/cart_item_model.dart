class CartItemModel {
  final int id;
  final int productId;
  final String productName;
  final String? productImageUrl;
  final double unitPrice;
  final int quantity;
  final double subtotal;
  final int availableStock;

  CartItemModel({
    required this.id,
    required this.productId,
    required this.productName,
    this.productImageUrl,
    required this.unitPrice,
    required this.quantity,
    required this.subtotal,
    required this.availableStock,
  });

  factory CartItemModel.fromJson(Map<String, dynamic> json) {
    return CartItemModel(
      id: json['id'] as int,
      productId: json['productId'] as int,
      productName: json['productName'] as String,
      productImageUrl: json['productImageUrl'] as String?,
      unitPrice: (json['unitPrice'] as num).toDouble(),
      quantity: json['quantity'] as int,
      subtotal: (json['subtotal'] as num).toDouble(),
      availableStock: json['availableStock'] as int,
    );
  }
}