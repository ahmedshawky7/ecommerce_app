import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/styling/app_assets.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/styling/theme_cubit.dart';
import 'package:ecommerce_app/core/widgets/logout_dialog.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/account/widgets/account_item_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ThemeCubit, ThemeMode>(
      builder: (context, themeMode) {
        final isLight = themeMode == ThemeMode.light;
        return Scaffold(
          appBar: AppBar(
            title: Text("Account"),
            centerTitle: true,
            actions: [
              IconButton(
                onPressed: () {
                  context.read<ThemeCubit>().toggleTheme(context);
                },
                icon: Icon(isLight ? Icons.dark_mode : Icons.light_mode),
              ),
            ],
          ),
          body: Column(
            children: [
              Expanded(
                child: ListView(
                  children: [
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 24.w),
                      child: Divider(color: Theme.of(context).dividerColor),
                    ),
                    AccountItemWidget(
                      title: 'My Orders',
                      iconPath: AppAssets.box,
                      onTap: () {},
                    ),
                    Divider(
                      thickness: 8,
                      color: Theme.of(context)
                          .dividerColor
                          .withOpacity(0.6),
                    ),
                    AccountItemWidget(
                      title: 'My Details',
                      iconPath: AppAssets.details,
                      onTap: () {},
                    ),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 24.w),
                      child: Divider(color: Theme.of(context).dividerColor),
                    ),
                    AccountItemWidget(
                      title: 'Address Book',
                      iconPath: AppAssets.address,
                      onTap: () {
                        GoRouter.of(context)
                            .pushNamed(AppRoutes.addressScreen);
                      },
                    ),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 24.w),
                      child: Divider(color: Theme.of(context).dividerColor),
                    ),
                    AccountItemWidget(
                      title: 'FAQs',
                      iconPath: AppAssets.question,
                      onTap: () {},
                    ),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 24.w),
                      child: Divider(color: Theme.of(context).dividerColor),
                    ),
                    AccountItemWidget(
                      title: 'Help Center',
                      iconPath: AppAssets.headphone,
                      onTap: () {},
                    ),
                  ],
                ),
              ),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 20.h),
                child: InkWell(
                  onTap: () {
                    showLogoutDialog(context);
                  },
                  child: Row(
                    children: [
                      Icon(
                        Icons.logout,
                        color: Theme.of(context).colorScheme.error,
                        size: 25.sp,
                      ),
                      WidthSpace(10),
                      Text(
                        'Logout',
                        style: AppStyles.black16w500Style(context).copyWith(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}