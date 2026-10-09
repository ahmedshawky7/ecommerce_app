import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:frontend_customer/features/checkout/presentation/pages/payment_cancel_page.dart';
import 'package:frontend_customer/features/checkout/presentation/pages/payment_success_page.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';
import 'features/auth/presentation/pages/login_page.dart';
import 'features/products/presentation/pages/home_page.dart';

class CustomerApp extends StatelessWidget {
  const CustomerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'E-Commerce Store',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      // Route Handling
      onGenerateRoute: (settings) {
        // دعم React-style hash routes
        if (settings.name == '/payment-success') {
          return MaterialPageRoute(builder: (_) => const PaymentSuccessPage());
        }
        if (settings.name == '/payment-cancel') {
          return MaterialPageRoute(builder: (_) => const PaymentCancelPage());
        }
        return null;
      },
      home: BlocBuilder<AuthCubit, AuthState>(
        builder: (context, state) {
          if (state is AuthAuthenticated) {
            return const HomePage();
          }
          return const LoginPage();
        },
      ),
    );
  }
}