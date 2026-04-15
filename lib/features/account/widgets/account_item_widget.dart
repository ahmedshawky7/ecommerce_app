import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class AccountItemWidget extends StatelessWidget {
  final String title;
  final String? iconPath;
  final Function()? onTap;

  const AccountItemWidget({
    super.key,
    required this.title,
    this.iconPath,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(12.r),
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 15.h),
          child: Row(
            children: [
              if (iconPath != null) ...[
                Image.asset(
                  iconPath!,
                  width: 25.w,
                  height: 25.h,

                  color: theme.colorScheme.onBackground,
                ),
                WidthSpace(20),
              ],

              Text(
                title,
                style: AppStyles.black16w500Style(context).copyWith(
                  color: theme.colorScheme.onBackground,
                ),
              ),

              const Spacer(),
              Icon(
                Icons.chevron_right,
                color: theme.hintColor,
                size: 18.sp,
              ),
            ],
          ),
        ),
      ),
    );
  }
}