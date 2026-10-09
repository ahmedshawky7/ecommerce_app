class ApiConstants {
  ApiConstants._();

  // ⚠️ ملاحظة: لو بتشغل على محاكي Android، استبدل localhost بـ 10.0.2.2
  static const String baseUrl = 'http://localhost:8080';
  
  // Auth
  static const String register = '/api/auth/register';
  static const String login = '/api/auth/login';
  static const String refresh = '/api/auth/refresh';
  
  // Products & Categories
  static const String products = '/api/products';
  static const String categories = '/api/categories';
  
  // Cart
  static const String cart = '/api/cart';
  static const String cartItems = '/api/cart/items';
  
  // Orders
  static const String orders = '/api/orders';
  static const String checkout = '/api/orders/checkout';
  
  // Storage Keys
  static const String tokenKey = 'access_token';
  static const String refreshTokenKey = 'refresh_token';
}