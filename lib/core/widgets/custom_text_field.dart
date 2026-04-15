import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class CustomTextField extends StatelessWidget {
  final String? hintText;
  final Widget? prefixIcon;
  final Widget? suffixIcon;
  final double? width;
  final bool? isPassword;
  final TextEditingController? controller;
  final String? Function(String?)? validator;
  final TextInputType? keyboardType;
  final Iterable<String>? autofillHints;
  final TextInputAction? textInputAction;
  final void Function(String)? onFieldSubmitted;

  const CustomTextField({
    super.key,
    this.hintText,
    this.prefixIcon,
    this.suffixIcon,
    this.width,
    this.isPassword,
    this.controller,
    this.validator,
    this.keyboardType,
    this.autofillHints,
    this.textInputAction,
    this.onFieldSubmitted,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SizedBox(
      width: width ?? 331.w,
      child: TextFormField(
        controller: controller,
        validator: validator,
        cursorColor: theme.colorScheme.primary,
        obscureText: isPassword ?? false,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        keyboardType: keyboardType,
        autofillHints: autofillHints,
        textInputAction: textInputAction ?? TextInputAction.next,
        onFieldSubmitted: onFieldSubmitted,
        onTapOutside: (_) => FocusScope.of(context).unfocus(),

        decoration: InputDecoration(
          filled: true,

          // ✅ أهم تعديل
          fillColor: theme.colorScheme.surface,

          hintText: hintText ?? '',
          hintStyle: TextStyle(
            color: theme.hintColor,
            fontSize: 15.sp,
            fontWeight: FontWeight.w500,
          ),

          prefixIcon: prefixIcon,
          suffixIcon: suffixIcon,

          contentPadding:
              EdgeInsets.symmetric(horizontal: 18.w, vertical: 18.h),

          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8.r),
            borderSide: BorderSide(
              color: theme.dividerColor, // 👈 ديناميك
              width: 1,
            ),
          ),

          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8.r),
            borderSide: BorderSide(
              color: theme.colorScheme.primary,
              width: 1.2,
            ),
          ),

          errorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8.r),
            borderSide: BorderSide(
              color: theme.colorScheme.error,
              width: 1,
            ),
          ),

          focusedErrorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8.r),
            borderSide: BorderSide(
              color: theme.colorScheme.error,
              width: 1.2,
            ),
          ),
        ),
      ),
    );
  }
}