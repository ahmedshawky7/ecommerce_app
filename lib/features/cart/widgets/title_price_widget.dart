import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class TitlePriceWidget extends StatelessWidget {
  final String title;
  final String price;
  final bool? isBold;

  const TitlePriceWidget({
    super.key,
    required this.title,
    required this.price,
    this.isBold,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: 10.h),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: isBold == true
                ? AppStyles.black15BoldStyle(context)
                : AppStyles.grey12MediumStyle(context).copyWith(
                    fontSize: 16.sp,
                    fontWeight: FontWeight.bold,
                  ),
          ),
          Spacer(),
          Text(price, style: AppStyles.black15BoldStyle(context)),
        ],
      ),
    );
  }
}