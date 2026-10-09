import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../constants/api_constants.dart';

class SecureStorage {
  SecureStorage._();
  static final SecureStorage instance = SecureStorage._();

  final _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future<void> saveToken(String token) async =>
      await _storage.write(key: ApiConstants.tokenKey, value: token);

  Future<String?> getToken() async =>
      await _storage.read(key: ApiConstants.tokenKey);

  Future<void> saveRefreshToken(String token) async =>
      await _storage.write(key: ApiConstants.refreshTokenKey, value: token);

  Future<String?> getRefreshToken() async =>
      await _storage.read(key: ApiConstants.refreshTokenKey);

  Future<void> clearAll() async => await _storage.deleteAll();
}