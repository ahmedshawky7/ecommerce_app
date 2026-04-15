import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class PrimaryButtonWidget extends StatelessWidget {
  final String? buttonText;
  final Color? backgroundColor;
  final Color? textColor;
  final double? width;
  final double? height;
  final double? fontSize;
  final double? borderRadius;
  final void Function()? onPressed;
  final Widget? icon;
  final Widget? trailingIcon;
  final bool isLoading;

  const PrimaryButtonWidget({
    super.key,
    this.buttonText,
    this.backgroundColor,
    this.textColor,
    this.width,
    this.height,
    this.borderRadius,
    this.onPressed,
    this.fontSize,
    this.icon,
    this.trailingIcon,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDisabled = onPressed == null || isLoading;

    return ElevatedButton(
      onPressed: isDisabled ? null : onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: isDisabled
            ? theme.disabledColor
            : backgroundColor ?? theme.colorScheme.primary,
        foregroundColor: textColor ?? theme.colorScheme.onPrimary,
        fixedSize: Size(width ?? 331.w, height ?? 56.h),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(borderRadius ?? 8.r),
        ),
        elevation: 0,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (icon != null) ...[
            IconTheme(
              data: IconThemeData(
                color: textColor ?? theme.colorScheme.onPrimary,
                size: 18.sp,
              ),
              child: icon!,
            ),
            WidthSpace(8),
          ],

          if (isLoading)
            SizedBox(
              width: 18.w,
              height: 18.h,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                valueColor:
                    AlwaysStoppedAnimation(theme.colorScheme.onPrimary),
                backgroundColor:
                    (theme.colorScheme.primary).withOpacity(0.3),
              ),
            )
          else
            Text(
              buttonText ?? '',
              style: TextStyle(
                fontSize: fontSize ?? 16.sp,
                fontWeight: FontWeight.w600,
                color: textColor ?? theme.colorScheme.onPrimary,
              ),
            ),

          if (trailingIcon != null) ...[
            WidthSpace(8),
            IconTheme(
              data: IconThemeData(
                color: textColor ?? theme.colorScheme.onPrimary,
                size: 18.sp,
              ),
              child: trailingIcon!,
            ),
          ],
        ],
      ),
    );
  }
}