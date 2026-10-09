class ProductModel {
  final int id;
  final String name;
  final String? description;
  final double price;
  final int stockQuantity;
  final String? imageUrl;
  final bool isActive;
  final int categoryId;
  final String categoryName;
  final int sellerId;
  final String sellerName;

  ProductModel({
    required this.id,
    required this.name,
    this.description,
    required this.price,
    required this.stockQuantity,
    this.imageUrl,
    required this.isActive,
    required this.categoryId,
    required this.categoryName,
    required this.sellerId,
    required this.sellerName,
  });

  factory ProductModel.fromJson(Map<String, dynamic> json) {
    return ProductModel(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      price: (json['price'] as num).toDouble(),
      stockQuantity: json['stockQuantity'] as int,
      imageUrl: json['imageUrl'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      categoryId: json['categoryId'] as int,
      categoryName: json['categoryName'] as String,
      sellerId: json['sellerId'] as int,
      sellerName: json['sellerName'] as String,
    );
  }
}