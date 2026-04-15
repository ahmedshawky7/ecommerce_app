import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class AddressItemWidget extends StatelessWidget {
  final String addressname;
  final String address;

  const AddressItemWidget({
    super.key,
    required this.addressname,
    required this.address,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: 10.h),
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 39.w, vertical: 16.h),
        width: double.infinity,
        height: 80.h,
        decoration: BoxDecoration(
          border: Border.all(
            color: Theme.of(context).colorScheme.secondary,
            width: 1.w,
          ),
          borderRadius: BorderRadius.circular(8.r),
        ),
        child: Row(
          children: [
            Icon(
              Icons.location_on_outlined,
              size: 24.sp,
              color: Theme.of(context).colorScheme.secondary,
            ),
            WidthSpace(12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    addressname,
                    style: AppStyles.black16w500Style(context),
                  ),
                  Text(
                    address,
                    maxLines: 1,
                    style: AppStyles.grey12MediumStyle(context),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}