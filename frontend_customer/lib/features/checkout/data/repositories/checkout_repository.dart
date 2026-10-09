import 'package:dio/dio.dart';
import '../../../../core/constants/api_constants.dart';
import '../../../../core/network/dio_client.dart';

class CheckoutSessionModel {
  final String sessionId;
  final String sessionUrl;
  final int orderId;
  final String orderNumber;

  CheckoutSessionModel({
    required this.sessionId,
    required this.sessionUrl,
    required this.orderId,
    required this.orderNumber,
  });

  factory CheckoutSessionModel.fromJson(Map<String, dynamic> json) {
    return CheckoutSessionModel(
      sessionId: json['sessionId'] as String,
      sessionUrl: json['sessionUrl'] as String,
      orderId: json['orderId'] as int,
      orderNumber: json['orderNumber'] as String,
    );
  }
}

class CheckoutRepository {
  final _dio = DioClient.instance.client;

  Future<CheckoutSessionModel> createCheckoutSession({
    required String shippingAddress,
    required String phone,
    required String successUrl,
    required String cancelUrl,
  }) async {
    try {
      final response = await _dio.post(
        '/api/orders/create-checkout-session',
        queryParameters: {
          'successUrl': successUrl,
          'cancelUrl': cancelUrl,
        },
        data: {
          'shippingAddress': shippingAddress,
          'phone': phone,
          'paymentMethod': 'STRIPE',
        },
      );
      return CheckoutSessionModel.fromJson(response.data);
    } on DioException catch (e) {
      if (e.response?.data is Map && e.response!.data['message'] != null) {
        throw e.response!.data['message'];
      }
      throw 'Failed to create checkout session';
    }
  }
}