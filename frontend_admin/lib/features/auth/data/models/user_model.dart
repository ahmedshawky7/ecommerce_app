class UserModel {
  final int id;
  final String email;
  final String username;
  final String role;
  final String token;
  final String refreshToken;

  UserModel({
    required this.id,
    required this.email,
    required this.username,
    required this.role,
    required this.token,
    required this.refreshToken,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      email: json['email'] as String,
      username: json['username'] as String,
      role: json['role'] as String? ?? 'CUSTOMER',
      token: json['token'] as String,
      refreshToken: json['refreshToken'] as String,
    );
  }

  bool get isAdmin => role == 'ADMIN';
  bool get isCustomer => role == 'CUSTOMER';
}