import 'package:ecommerce_app/core/styling/app_fonts.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class AppStyles {
  static TextStyle primaryHeadLineStyle(BuildContext context) => TextStyle(
        fontSize: 30.sp,
        fontWeight: FontWeight.bold,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).textTheme.titleLarge?.color,
      );

  static TextStyle subTitleStyle(BuildContext context) => TextStyle(
        fontSize: 16.sp,
        fontWeight: FontWeight.w400,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).textTheme.titleMedium?.color,
      );

  static TextStyle black16w500Style(BuildContext context) => TextStyle(
        fontSize: 16.sp,
        fontWeight: FontWeight.w500,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).colorScheme.onBackground,
      );

  static TextStyle grey12MediumStyle(BuildContext context) => TextStyle(
        fontSize: 14.sp,
        fontWeight: FontWeight.w400,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).colorScheme.secondary,
      );

  static TextStyle black15BoldStyle(BuildContext context) => TextStyle(
        fontSize: 15.sp,
        fontWeight: FontWeight.bold,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).colorScheme.onBackground,
      );

  static TextStyle white24BoldStyle(BuildContext context) => TextStyle(
        fontSize: 24.sp,
        fontWeight: FontWeight.bold,
        fontFamily: AppFonts.mainFontName,
        color: Theme.of(context).colorScheme.onPrimary,
      );
}