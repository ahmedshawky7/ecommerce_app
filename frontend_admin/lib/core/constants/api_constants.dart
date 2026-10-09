class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'http://localhost:8080';
  
  // Auth Endpoints
  static const String login = '/api/auth/login';
  static const String register = '/api/auth/register';
  static const String refresh = '/api/auth/refresh';
  
  // Admin Endpoints
  static const String dashboardStats = '/api/admin/dashboard/stats';
  static const String topProducts = '/api/admin/dashboard/top-products';
  static const String lowStock = '/api/admin/inventory/low-stock';
  static const String adminOrders = '/api/admin/orders';
  static const String adminUsers = '/api/admin/users';
  static const String salesAnalytics = '/api/admin/analytics/sales';
  static const String revenueByCategory = '/api/admin/analytics/revenue-by-category';
  
  // Storage Keys
  static const String tokenKey = 'access_token';
  static const String refreshTokenKey = 'refresh_token';
  static const String userKey = 'user_data';
}