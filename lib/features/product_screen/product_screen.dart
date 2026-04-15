import 'package:animated_snack_bar/animated_snack_bar.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:ecommerce_app/core/styling/app_color.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/utils/animated_snack_dialog.dart';
import 'package:ecommerce_app/core/widgets/primary_button_widget.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/cart/cubit/cart_cubit.dart';
import 'package:ecommerce_app/features/cart/cubit/cart_state.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class ProductScreen extends StatelessWidget {
  final Product product;
  const ProductScreen({super.key, required this.product});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          // SliverAppBar مع تأثير التمدد
          SliverAppBar(
            expandedHeight: 400.h,
            pinned: true,
            stretch: true,
            centerTitle: true,
            title: Text(
              product.title,
              style: AppStyles.black15BoldStyle(context),
            ),
            flexibleSpace: FlexibleSpaceBar(
              background: Hero(
                tag: '${product.thumbnail}-${product.title}',
                child: CachedNetworkImage(
                  imageUrl: product.thumbnail,
                  fit: BoxFit.cover,
                  placeholder: (context, url) => Container(
                    color: Colors.grey[200],
                    child: const Center(child: CircularProgressIndicator()),
                  ),
                  errorWidget: (context, url, error) => Container(
                    color: Colors.grey[200],
                    child: const Icon(Icons.error, size: 50),
                  ),
                ),
              ),
            ),
          ),
          
          // محتوى المنتج
          SliverPadding(
            padding: EdgeInsets.symmetric(horizontal: 24.w),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                HeightSpace(20),
                
                // التقييمات
                Row(
                  children: [
                    Icon(Icons.star, color: AppColor.yellowColor, size: 20.sp),
                    WidthSpace(4),
                    Text(
                      product.rating.toString(),
                      style: AppStyles.black16w500Style(context).copyWith(
                        fontSize: 16.sp,
                        decoration: TextDecoration.underline,
                      ),
                    ),
                    WidthSpace(4),
                    Text(
                      '${product.reviews?.length ?? 0} Reviews',
                      style: AppStyles.grey12MediumStyle(context).copyWith(
                        fontSize: 16.sp,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                
                HeightSpace(16),
                
                // الوصف
                Text(
                  'Description',
                  style: AppStyles.black16w500Style(context).copyWith(
                    fontSize: 18.sp,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                HeightSpace(8),
                Text(
                  product.description,
                  style: AppStyles.grey12MediumStyle(context).copyWith(
                    fontSize: 16.sp,
                    height: 1.5,
                  ),
                ),
                
                HeightSpace(30),
                
                // مواصفات إضافية (يمكن إضافتها من الـ API)
                _buildSpecsSection(context),
                
                HeightSpace(50), // مساحة للـ bottom bar
              ]),
            ),
          ),
        ],
      ),
      
      // Bottom Bar ثابت
      bottomNavigationBar: _buildBottomBar(context),
    );
  }

  Widget _buildSpecsSection(context) {
  return Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        'Specifications',
        style: AppStyles.black16w500Style(context).copyWith(
          fontSize: 18.sp,
          fontWeight: FontWeight.bold,
        ),
      ),
      HeightSpace(12),
      Container(
        padding: EdgeInsets.all(16.w),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surface,
          borderRadius: BorderRadius.circular(12.r),
          border: Border.all(color: Theme.of(context).dividerColor),
        ),
        child: Column(
          children: [
            _buildSpecRow(context, 'Brand', product.brand ?? 'N/A'),
            _buildSpecRow(context, 'Category', product.category),
            _buildSpecRow(context, 'Stock', '${product.stock} units'),
            _buildSpecRow(context, 'Discount', '${product.discountPercentage}%'),
          ],
        ),
      ),
    ],
  );
}

 Widget _buildSpecRow(BuildContext context, String label, String value) {
  return Padding(
    padding: EdgeInsets.symmetric(vertical: 8.h),
    child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppStyles.grey12MediumStyle(context).copyWith(
            fontSize: 14.sp,
          ),
        ),
        Text(
          value,
          style: AppStyles.black16w500Style(context).copyWith(
            fontSize: 14.sp,
          ),
        ),
      ],
    ),
  );
}

  Widget _buildBottomBar(BuildContext context) {
  final theme = Theme.of(context);

  return Container(
    padding: EdgeInsets.symmetric(horizontal: 24.w, vertical: 12.h),
    decoration: BoxDecoration(
      color: theme.colorScheme.surface,
      border: Border(top: BorderSide(color: theme.dividerColor)),
      boxShadow: [
        BoxShadow(
          color: theme.shadowColor.withOpacity(0.05),
          spreadRadius: 1,
          blurRadius: 10,
          offset: const Offset(0, -3),
        ),
      ],
    ),
    child: SafeArea(
      child: Row(
        children: [
          // السعر
          Container(
            padding: EdgeInsets.symmetric(horizontal: 16.w, vertical: 8.h),
            decoration: BoxDecoration(
              color: theme.colorScheme.primary.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12.r),
            ),
            child: Text(
              '\$${product.price}',
              style: AppStyles.black16w500Style(context).copyWith(
                fontSize: 24.sp,
                color: theme.colorScheme.primary,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          WidthSpace(16),

          // زر الإضافة
          Expanded(
            child: BlocConsumer<CartCubit, CartState>(
              listener: (context, state) {
                if (state is SuccessAddingToCart) {
                  showAnimatedSnackDialog(
                    context,
                    message: 'Product Added Successfully to Cart',
                    type: AnimatedSnackBarType.success,
                  );
                }
              },
              builder: (context, state) {
                return PrimaryButtonWidget(
                  isLoading: state is AddingToCart,
                  buttonText: 'Add to Cart',
                  icon: Icon(
                    Icons.shopping_cart_outlined,
                    color: theme.colorScheme.onPrimary,
                    size: 24.sp,
                  ),
                  onPressed: state is AddingToCart
                      ? null
                      : () {
                          context.read<CartCubit>().addingToCart(product, 1);
                        },
                );
              },
            ),
          ),
        ],
      ),
    ),
  );
}
}