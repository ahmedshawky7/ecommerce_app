import 'order_item_model.dart';

class OrderModel {
  final int id;
  final String orderNumber;
  final double totalAmount;
  final String status;
  final String shippingAddress;
  final String phone;
  final String? paymentMethod;
  final String? paymentIntentId;
  final int customerId;
  final String customerName;
  final String customerEmail;
  final List<OrderItemModel> items;
  final DateTime createdAt;
  final DateTime updatedAt;

  OrderModel({
    required this.id,
    required this.orderNumber,
    required this.totalAmount,
    required this.status,
    required this.shippingAddress,
    required this.phone,
    this.paymentMethod,
    this.paymentIntentId,
    required this.customerId,
    required this.customerName,
    required this.customerEmail,
    required this.items,
    required this.createdAt,
    required this.updatedAt,
  });

  factory OrderModel.fromJson(Map<String, dynamic> json) {
    return OrderModel(
      id: json['id'] as int,
      orderNumber: json['orderNumber'] as String,
      totalAmount: (json['totalAmount'] as num).toDouble(),
      status: json['status'] as String,
      shippingAddress: json['shippingAddress'] as String,
      phone: json['phone'] as String,
      paymentMethod: json['paymentMethod'] as String?,
      paymentIntentId: json['paymentIntentId'] as String?,
      customerId: json['customerId'] as int,
      customerName: json['customerName'] as String,
      customerEmail: json['customerEmail'] as String,
      items: (json['items'] as List)
          .map((e) => OrderItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }
}