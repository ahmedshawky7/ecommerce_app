class CartModel {
  int? id;
  List<CartItem> products; // غيرنا النوع هنا
  double? total;
  double? discountedTotal;
  int? userId;

  CartModel({
    this.id,
    required this.products,
    this.total,
    this.discountedTotal,
    this.userId,
  });

  double get shippingFee => (total != null && total! > 1000) ? 0.0 : 50.0;

  double get tax => (total ?? 0) * 0.14;

  double get finalGrandTotal => (total ?? 0) + shippingFee + tax;

  factory CartModel.fromJson(Map<String, dynamic> json) => CartModel(
    id: json["id"],
    products: json["products"] != null
        ? List<CartItem>.from(json["products"].map((x) => CartItem.fromJson(x)))
        : [],
    total: (json["total"] as num?)?.toDouble(),
    discountedTotal: (json["discountedTotal"] as num?)?.toDouble(),
    userId: json["userId"],
  );
}

class CartItem {
  int id;
  String title;
  double price;
  int quantity;
  double total;
  String thumbnail;

  CartItem({
    required this.id,
    required this.title,
    required this.price,
    required this.quantity,
    required this.total,
    required this.thumbnail,
  });

  factory CartItem.fromJson(Map<String, dynamic> json) => CartItem(
    id: json["id"],
    title: json["title"] ?? "",
    price: (json["price"] as num).toDouble(),
    quantity: json["quantity"],
    total: (json["total"] as num).toDouble(),
    thumbnail: json["thumbnail"] ?? "",
  );
}
