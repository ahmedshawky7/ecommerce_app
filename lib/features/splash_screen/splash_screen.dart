import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/styling/app_assets.dart';
import 'package:ecommerce_app/core/utils/service_locator.dart';
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with TickerProviderStateMixin {
  AnimationController? animationController;
  Animation<double>? animation;
 @override
void initState() {
  super.initState();

  animationController = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 1),
  )..repeat(reverse: true);

  animation = CurvedAnimation(
    parent: animationController!,
    curve: Curves.fastOutSlowIn,
  );

  // الانتقال بعد 2 ثانية
  Future.delayed(const Duration(seconds: 2), checkToken);
}

Future<void> checkToken() async {
  final token = await sl<StorageHelper>().getToken();

  if (token != null && token.isNotEmpty) {
    context.goNamed(AppRoutes.mainScreen);
  } else {
    context.goNamed(AppRoutes.loginScreen);
  }
}

 Future delay() async {
  await Future.delayed(const Duration(seconds: 2));

  final token = await sl<StorageHelper>().getToken();

  if (token != null && token.isNotEmpty) {
    context.goNamed(AppRoutes.mainScreen);
  } else {
    context.goNamed(AppRoutes.loginScreen);
  }
}

  @override
  void dispose() {
    animationController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ScaleTransition(
          scale: animation!,
          child: Image.asset(AppAssets.logo, width: 200.w, height: 200.h),
        ),
      ),
    );
  }
}
