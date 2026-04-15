import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class StorageHelper {
  final storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future saveToken({required String value}) async {
    return await storage.write(key: 'token', value: value);
  }

  Future<String?> getToken() async {
    return await storage.read(key: 'token');
  }

  Future removeToken() async {
    return await storage.delete(key: 'token');
  }

  Future<void> saveThemeMode(ThemeMode mode) async {
    await storage.write(key: 'theme_mode', value: mode.name); // mode.name هيخزن "light" أو "dark"
  }

  Future<ThemeMode> getThemeMode() async {
    String? themeStr = await storage.read(key: 'theme_mode');
    if (themeStr == 'light') return ThemeMode.light;
    if (themeStr == 'dark') return ThemeMode.dark;
    return ThemeMode.system;
}
}