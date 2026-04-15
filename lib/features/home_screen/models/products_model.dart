class ProductsModel {
  List<Product> products;
  int total;
  int skip;
  int limit;

  ProductsModel({
    required this.products,
    required this.total,
    required this.skip,
    required this.limit,
  });

  factory ProductsModel.fromJson(Map<String, dynamic> json) {
    return ProductsModel(
      products: List<Product>.from(
        json["products"].map((x) => Product.fromJson(x)),
      ),
      total: json["total"],
      skip: json["skip"],
      limit: json["limit"],
    );
  }
}


class Product {
  int id;
  String title;
  String description;
  double price;
  double rating;
  String category;
  String thumbnail;
  List<String> images;

  double? discountPercentage;
  int? stock;
  String? brand;
  String? sku;
  int? weight;

  Dimensions? dimensions;
  List<String>? tags;
  List<Review>? reviews;

  String? warrantyInformation;
  String? shippingInformation;
  String? availabilityStatus;
  String? returnPolicy;
  int? minimumOrderQuantity;

  Meta? meta;

  Product({
    required this.id,
    required this.title,
    required this.description,
    required this.price,
    required this.rating,
    required this.category,
    required this.thumbnail,
    required this.images,
    this.discountPercentage,
    this.stock,
    this.brand,
    this.sku,
    this.weight,
    this.dimensions,
    this.tags,
    this.reviews,
    this.warrantyInformation,
    this.shippingInformation,
    this.availabilityStatus,
    this.returnPolicy,
    this.minimumOrderQuantity,
    this.meta,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json["id"],
      title: json["title"],
      description: json["description"],
      price: (json["price"] as num).toDouble(),
      rating: (json["rating"] as num).toDouble(),
      category: json["category"],
      thumbnail: json["thumbnail"],
      images: List<String>.from(json["images"]),

      discountPercentage:
          (json["discountPercentage"] as num?)?.toDouble(),
      stock: json["stock"],
      brand: json["brand"],
      sku: json["sku"],
      weight: json["weight"],

      tags: json["tags"] != null
          ? List<String>.from(json["tags"])
          : [],

      dimensions: json["dimensions"] != null
          ? Dimensions.fromJson(json["dimensions"])
          : null,

      reviews: json["reviews"] != null
          ? List<Review>.from(
              json["reviews"].map((x) => Review.fromJson(x)))
          : [],

      warrantyInformation: json["warrantyInformation"],
      shippingInformation: json["shippingInformation"],
      availabilityStatus: json["availabilityStatus"],
      returnPolicy: json["returnPolicy"],
      minimumOrderQuantity: json["minimumOrderQuantity"],

      meta: json["meta"] != null
          ? Meta.fromJson(json["meta"])
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "id": id,
      "title": title,
      "description": description,
      "price": price,
      "rating": rating,
      "category": category,
      "thumbnail": thumbnail,
      "images": images,
      "discountPercentage": discountPercentage,
      "stock": stock,
      "brand": brand,
      "sku": sku,
      "weight": weight,
      "tags": tags,
      "dimensions": dimensions?.toJson(),
      "reviews": reviews?.map((e) => e.toJson()).toList(),
      "warrantyInformation": warrantyInformation,
      "shippingInformation": shippingInformation,
      "availabilityStatus": availabilityStatus,
      "returnPolicy": returnPolicy,
      "minimumOrderQuantity": minimumOrderQuantity,
      "meta": meta?.toJson(),
    };
  }
}


class Dimensions {
  double width;
  double height;
  double depth;

  Dimensions({
    required this.width,
    required this.height,
    required this.depth,
  });

  factory Dimensions.fromJson(Map<String, dynamic> json) {
    return Dimensions(
      width: (json["width"] as num).toDouble(),
      height: (json["height"] as num).toDouble(),
      depth: (json["depth"] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "width": width,
      "height": height,
      "depth": depth,
    };
  }
}

class Review {
  int rating;
  String comment;
  String date;
  String reviewerName;
  String reviewerEmail;

  Review({
    required this.rating,
    required this.comment,
    required this.date,
    required this.reviewerName,
    required this.reviewerEmail,
  });

  factory Review.fromJson(Map<String, dynamic> json) {
    return Review(
      rating: json["rating"],
      comment: json["comment"],
      date: json["date"],
      reviewerName: json["reviewerName"],
      reviewerEmail: json["reviewerEmail"],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "rating": rating,
      "comment": comment,
      "date": date,
      "reviewerName": reviewerName,
      "reviewerEmail": reviewerEmail,
    };
  }
}

class Meta {
  String createdAt;
  String updatedAt;
  String barcode;
  String qrCode;

  Meta({
    required this.createdAt,
    required this.updatedAt,
    required this.barcode,
    required this.qrCode,
  });

  factory Meta.fromJson(Map<String, dynamic> json) {
    return Meta(
      createdAt: json["createdAt"],
      updatedAt: json["updatedAt"],
      barcode: json["barcode"],
      qrCode: json["qrCode"],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "createdAt": createdAt,
      "updatedAt": updatedAt,
      "barcode": barcode,
      "qrCode": qrCode,
    };
  }
}