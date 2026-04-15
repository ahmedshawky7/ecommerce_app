// lib/core/styling/theme_cubit.dart
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class ThemeCubit extends Cubit<ThemeMode> {
  final StorageHelper _storageHelper;

  ThemeCubit(this._storageHelper) : super(ThemeMode.system);

  // دالة بنناديها أول ما التطبيق يفتح عشان يحمل الثيم القديم
  Future<void> loadTheme() async {
    final savedMode = await _storageHelper.getThemeMode();
    emit(savedMode);
  }

  void toggleTheme(BuildContext context) async {
    final Brightness currentBrightness = Theme.of(context).brightness;
    ThemeMode newMode;

    if (currentBrightness == Brightness.light) {
      newMode = ThemeMode.dark;
    } else {
      newMode = ThemeMode.light;
    }

    emit(newMode);
    await _storageHelper.saveThemeMode(newMode); // حفظ الاختيار
  }
}