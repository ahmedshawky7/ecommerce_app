class DashboardStatsModel {
  final double totalRevenue;
  final int totalOrders;
  final int totalCustomers;
  final int totalProducts;
  final int pendingOrders;
  final int confirmedOrders;
  final int cancelledOrders;

  DashboardStatsModel({
    required this.totalRevenue,
    required this.totalOrders,
    required this.totalCustomers,
    required this.totalProducts,
    required this.pendingOrders,
    required this.confirmedOrders,
    required this.cancelledOrders,
  });

  factory DashboardStatsModel.fromJson(Map<String, dynamic> json) {
    return DashboardStatsModel(
      totalRevenue: (json['totalRevenue'] as num).toDouble(),
      totalOrders: json['totalOrders'] as int,
      totalCustomers: json['totalCustomers'] as int,
      totalProducts: json['totalProducts'] as int,
      pendingOrders: json['pendingOrders'] as int,
      confirmedOrders: json['confirmedOrders'] as int,
      cancelledOrders: json['cancelledOrders'] as int,
    );
  }
}