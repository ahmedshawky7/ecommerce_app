class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'https://ecommerceapp-production-5c76.up.railway.app';
  
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