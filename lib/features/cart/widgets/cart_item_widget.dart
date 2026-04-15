import 'package:cached_network_image/cached_network_image.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/cart/models/cart_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class CartItemWidget extends StatelessWidget {
  final CartItem cartItem;

  const CartItemWidget({super.key, required this.cartItem});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: 16.h),
      child: Container(
        padding: EdgeInsets.all(16.sp),
        decoration: BoxDecoration(
          border: Border.all(
            color: Theme.of(context).colorScheme.secondary,
            width: 0.5.w,
          ),
          borderRadius: BorderRadius.circular(8.r),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(8.r),
              child: CachedNetworkImage(
                width: 83.w,
                height: 79.h,
                imageUrl: cartItem.thumbnail,
                fit: BoxFit.cover,
                placeholder: (context, url) => Container(
                  width: 83.w,
                  height: 79.h,
                  color: Theme.of(context).colorScheme.secondary,
                ),
              ),
            ),
            WidthSpace(16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      SizedBox(
                        width: 180.w,
                        child: Text(
                          cartItem.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AppStyles.black16w500Style(context)
                              .copyWith(fontSize: 14.sp),
                        ),
                      ),
                      Spacer(),
                      Icon(Icons.delete, color: Theme.of(context).colorScheme.error),
                    ],
                  ),
                  HeightSpace(30),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '\$${cartItem.price}',
                        style: AppStyles.black16w500Style(context),
                      ),
                      Spacer(),
                      Row(
                        children: [
                          _quantityButton(context, icon: Icons.remove, onTap: () {}),
                          WidthSpace(8),
                          Text(
                            '${cartItem.quantity}',
                            style: AppStyles.black16w500Style(context),
                          ),
                          WidthSpace(8),
                          _quantityButton(context, icon: Icons.add, onTap: () {}),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _quantityButton(BuildContext context,
      {required IconData icon, required VoidCallback onTap}) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: 24.w,
        height: 24.h,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border.all(
            color: Theme.of(context).colorScheme.secondary,
            width: 0.5.w,
          ),
          borderRadius: BorderRadius.circular(3.r),
        ),
        child: Icon(
          icon,
          size: 18.sp,
          color: Theme.of(context).colorScheme.onBackground,
        ),
      ),
    );
  }
}