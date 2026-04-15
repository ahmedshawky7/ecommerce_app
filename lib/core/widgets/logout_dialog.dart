import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/features/auth/cubit/auth_cubit.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

void showLogoutDialog(BuildContext parentContext) {
  final authCubit = parentContext.read<AuthCubit>();
  final theme = Theme.of(parentContext);

  showDialog<void>(
    context: parentContext,
    builder: (context) => AlertDialog(
      backgroundColor: theme.colorScheme.surface,
      icon: Icon(
        Icons.warning_amber_sharp,
        color: theme.colorScheme.error,
        size: 80.sp,
      ),
      title: Text(
        'Logout',
        style: AppStyles.primaryHeadLineStyle(context)
            .copyWith(color: theme.colorScheme.onSurface),
      ),
      content: Text(
        'Are you sure you want to logout?',
        style: AppStyles.black16w500Style(context)
            .copyWith(color: theme.colorScheme.onSurface),
      ),
      actions: <Widget>[
        TextButton(
          child: Text(
            'No',
            style: AppStyles.black15BoldStyle(context)
                .copyWith(color: theme.colorScheme.primary),
          ),
          onPressed: () => Navigator.of(context).pop(),
        ),
        TextButton(
          child: Text(
            'Yes',
            style: AppStyles.black15BoldStyle(context)
                .copyWith(color: theme.colorScheme.error),
          ),
          onPressed: () {
            authCubit.logout();
            Navigator.pop(context);
            parentContext.goNamed(AppRoutes.loginScreen);
          },
        ),
      ],
    ),
  );
}