import 'package:ecommerce_app/core/styling/theme_cubit.dart';
import 'package:ecommerce_app/core/widgets/primary_button_widget.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/cart/cubit/cart_cubit.dart';
import 'package:ecommerce_app/features/cart/cubit/cart_state.dart';
import 'package:ecommerce_app/features/cart/widgets/cart_item_widget.dart';
import 'package:ecommerce_app/features/cart/widgets/title_price_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:shimmer/shimmer.dart';

class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text("Cart"),
        actions: [
          BlocBuilder<ThemeCubit, ThemeMode>(
            builder: (context, themeMode) {
              return IconButton(
                onPressed: () {
                  context.read<ThemeCubit>().toggleTheme(context);
                },
                icon: Icon(
                  themeMode == ThemeMode.light
                      ? Icons.dark_mode
                      : Icons.light_mode,
                ),
              );
            },
          ),
        ],
        centerTitle: true,
      ),
      body: BlocBuilder<CartCubit, CartState>(
        builder: (context, state) {
          if (state is CartLoading) {
            return Shimmer.fromColors(
              baseColor: theme.colorScheme.secondary,
              highlightColor: theme.colorScheme.primary,
              child: Padding(
                padding: EdgeInsets.symmetric(horizontal: 24.0.w),
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      ...List.generate(
                        6,
                        (index) => Column(
                          children: [
                            Container(
                              width: double.infinity,
                              height: 120.h,
                              decoration: BoxDecoration(
                                color: theme.colorScheme.surface,
                                borderRadius: BorderRadius.circular(8.r),
                              ),
                            ),
                            HeightSpace(16),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }

          if (state is CartSuccess) {
            if (state.cartModel.products.isEmpty) {
              return Center(
                child: Text(
                  "Cart is empty",
                  style: TextStyle(
                    color: theme.colorScheme.onBackground,
                    fontSize: 16.sp,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              );
            }

            return SafeArea(
              child: Column(
                children: [
                  Expanded(
                    child: ListView.separated(
                      padding: EdgeInsets.symmetric(
                        horizontal: 24.w,
                        vertical: 20.h,
                      ),
                      itemCount: state.cartModel.products.length,
                      separatorBuilder: (context, index) => HeightSpace(16),
                      itemBuilder: (context, index) {
                        return CartItemWidget(
                          cartItem: state.cartModel.products[index],
                        );
                      },
                    ),
                  ),

                  // Bottom summary & checkout
                  Container(
                    padding: EdgeInsets.all(24.w),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surface,
                      borderRadius: BorderRadius.vertical(
                        top: Radius.circular(20.r),
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: theme.colorScheme.onBackground.withOpacity(
                            0.05,
                          ),
                          blurRadius: 10,
                        ),
                      ],
                    ),
                    child: Column(
                      children: [
                        TitlePriceWidget(
                          title: 'Sub Total',
                          price: "\$${state.cartModel.total}",
                        ),
                        TitlePriceWidget(
                          title: 'Shipping fee',
                          price: "\$50.0",
                        ),
                        Divider(height: 30.h, color: theme.dividerColor),
                        TitlePriceWidget(
                          title: 'Total',
                          price: "\$${(state.cartModel.total ?? 0) + 50}",
                          isBold: true,
                        ),
                        HeightSpace(20),
                        SizedBox(
                          width: double.infinity,
                          child: PrimaryButtonWidget(
                            buttonText: 'Go to checkout',
                            onPressed: () {
                              // Navigation to Payment or Checkout
                            },
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          }

          return Center(
            child: Text(
              'Error occurred',
              style: TextStyle(color: theme.colorScheme.error),
            ),
          );
        },
      ),
    );
  }
}
