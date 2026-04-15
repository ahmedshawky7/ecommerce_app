import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/address/widgets/addressitemwidget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class AddressScreen extends StatelessWidget {
  AddressScreen({super.key});
  final List<Map<String, String>> addresses = [
    {"name": "Home", "address": "925 S Chugach St #APT 10"},
    {"name": "Work", "address": "21 King Street"},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Address"), centerTitle: true),
      body: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.0.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Divider(),
            HeightSpace(20),
            Text(
              'Saved Address',
              style: AppStyles.black16w500Style(context).copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            HeightSpace(14),
            Expanded(
              child: ListView.builder(
                itemCount: addresses.length,
                itemBuilder: (context, index) {
                  return AddressItemWidget(
                    addressname: addresses[index]["name"]!,
                    address: addresses[index]["address"]!,
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
