import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/utils/service_locator.dart';
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:ecommerce_app/features/account/account_screen.dart';
import 'package:ecommerce_app/features/address/address_screen.dart';
import 'package:ecommerce_app/features/auth/login_screen.dart';
import 'package:ecommerce_app/features/auth/register_screen.dart';
import 'package:ecommerce_app/features/cart/cart_screen.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';
import 'package:ecommerce_app/features/main_screen/main_screen.dart';
import 'package:ecommerce_app/features/product_screen/product_screen.dart';
import 'package:ecommerce_app/features/splash_screen/splash_screen.dart';
import 'package:go_router/go_router.dart';

class RouterGenerationConfig {
  static final GoRouter goRoute = GoRouter(
    initialLocation: AppRoutes.splashScreen,  
    
    redirect: (context, state) async {
      final token = await sl<StorageHelper>().getToken();

      final bool isLoggedIn = token != null && token.isNotEmpty;

      // حالات التوجيه
      if (!isLoggedIn) {
        // مش مسجل → لازم يروح login إلا لو هو فعلاً في login/register/splash
        if (state.matchedLocation == AppRoutes.loginScreen ||
            state.matchedLocation == AppRoutes.registerScreen ||
            state.matchedLocation == AppRoutes.splashScreen) {
          return null;
        }
        return AppRoutes.loginScreen;
      }

      // مسجل دخول → ممنوع يشوف login/register
      if (state.matchedLocation == AppRoutes.loginScreen ||
          state.matchedLocation == AppRoutes.registerScreen) {
        return AppRoutes.mainScreen;
      }

      // كل حاجة تمام
      return null;
    },
    routes: [
      GoRoute(
        name: AppRoutes.splashScreen,
        path: AppRoutes.splashScreen,
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        name: AppRoutes.loginScreen,
        path: AppRoutes.loginScreen,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        name: AppRoutes.registerScreen,
        path: AppRoutes.registerScreen,
        builder: (context, state) => const RegisterScreen(),
      ),
      GoRoute(
        name: AppRoutes.mainScreen,
        path: AppRoutes.mainScreen,
        builder: (context, state) => const MainScreen(),
      ),
      GoRoute(
        name: AppRoutes.productScreen,
        path: AppRoutes.productScreen,
        builder: (context, state) {
          return ProductScreen(product: state.extra as Product);
        },
      ),
      GoRoute(
        name: AppRoutes.cartScreen,
        path: AppRoutes.cartScreen,
        builder: (context, state) => const CartScreen(),
      ),
      GoRoute(
        name: AppRoutes.accountScreen,
        path: AppRoutes.accountScreen,
        builder: (context, state) => const AccountScreen(),
      ),
      GoRoute(
        name: AppRoutes.addressScreen,
        path: AppRoutes.addressScreen,
        builder: (context, state) =>  AddressScreen(),
      ),
    ],
  );
}
