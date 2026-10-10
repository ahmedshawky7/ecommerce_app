class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'https://ecommerceapp-production-5c76.up.railway.app';

  // ============ Auth ============
  static const String login = '/api/auth/login';
  static const String register = '/api/auth/register';
  static const String refresh = '/api/auth/refresh';

  // ============ Products & Categories ============
  static const String products = '/api/products';
  static const String categories = '/api/categories';

  // ============ Cart ============
  static const String cart = '/api/cart';
  static const String cartItems = '/api/cart/items';

  // ============ Orders ============
  static const String orders = '/api/orders';
  static const String checkout = '/api/orders/checkout';
  static const String createCheckoutSession = '/api/orders/create-checkout-session';

  // ============ Admin ============
  static const String dashboardStats = '/api/admin/dashboard/stats';
  static const String topProducts = '/api/admin/dashboard/top-products';
  static const String lowStock = '/api/admin/inventory/low-stock';
  static const String adminOrders = '/api/admin/orders';
  static const String adminUsers = '/api/admin/users';
  static const String salesAnalytics = '/api/admin/analytics/sales';
  static const String revenueByCategory = '/api/admin/analytics/revenue-by-category';

  // ============ Storage Keys ============
  static const String tokenKey = 'access_token';
  static const String refreshTokenKey = 'refresh_token';
  static const String userKey = 'user_data';
}