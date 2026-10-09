class SalesAnalyticsModel {
  final String date;
  final int ordersCount;
  final double revenue;

  SalesAnalyticsModel({
    required this.date,
    required this.ordersCount,
    required this.revenue,
  });

  factory SalesAnalyticsModel.fromJson(Map<String, dynamic> json) {
    return SalesAnalyticsModel(
      date: json['date'] as String,
      ordersCount: json['ordersCount'] as int,
      revenue: (json['revenue'] as num).toDouble(),
    );
  }
}

class RevenueByCategoryModel {
  final int categoryId;
  final String categoryName;
  final int totalQuantitySold;
  final double totalRevenue;

  RevenueByCategoryModel({
    required this.categoryId,
    required this.categoryName,
    required this.totalQuantitySold,
    required this.totalRevenue,
  });

  factory RevenueByCategoryModel.fromJson(Map<String, dynamic> json) {
    return RevenueByCategoryModel(
      categoryId: json['categoryId'] as int,
      categoryName: json['categoryName'] as String,
      totalQuantitySold: json['totalQuantitySold'] as int,
      totalRevenue: (json['totalRevenue'] as num).toDouble(),
    );
  }
}