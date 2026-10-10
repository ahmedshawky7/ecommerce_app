import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/storage/secure_storage.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/main_layout.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';
import 'features/auth/presentation/pages/login_page.dart';
import 'features/cart/data/repositories/cart_repository.dart';
import 'features/cart/presentation/cubit/cart_cubit.dart';
import 'features/checkout/presentation/pages/payment_cancel_page.dart';
import 'features/checkout/presentation/pages/payment_success_page.dart';
import 'features/dashboard/data/repositories/dashboard_repository.dart';
import 'features/dashboard/presentation/cubit/dashboard_cubit.dart';
import 'features/products/presentation/pages/home_page.dart';

class AdminApp extends StatefulWidget {
  const AdminApp({super.key});

  @override
  State<AdminApp> createState() => _AdminAppState();
}

class _AdminAppState extends State<AdminApp> {
  String? _paymentResult;
  bool _checking = true;

  @override
  void initState() {
    super.initState();
    _checkPaymentResult();
  }

  Future<void> _checkPaymentResult() async {
    final result = await SecureStorage.instance.getPaymentResult();
    if (mounted) {
      setState(() {
        _paymentResult = result;
        _checking = false;
      });
    }
  }

  void _clearPaymentResult() {
    SecureStorage.instance.clearPaymentResult();
    setState(() => _paymentResult = null);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'E-Commerce',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: _checking
          ? const Scaffold(
              body: Center(child: CircularProgressIndicator()),
            )
          : _buildHome(),
    );
  }

  Widget _buildHome() {
    // Show payment result if exists
    if (_paymentResult == 'SUCCESS') {
      return PaymentSuccessPage(onBackToHome: _clearPaymentResult);
    }
    if (_paymentResult == 'CANCEL') {
      return PaymentCancelPage(onBackToHome: _clearPaymentResult);
    }

    return BlocBuilder<AuthCubit, AuthState>(
      builder: (context, state) {
        if (state is AuthAuthenticated) {
          if (state.user.isAdmin) {
            return BlocProvider(
              create: (_) => DashboardCubit(DashboardRepository()),
              child: const MainLayout(),
            );
          } else {
            return BlocProvider(
              create: (_) => CartCubit(CartRepository()),
              child: const HomePage(),
            );
          }
        }
        return const LoginPage();
      },
    );
  }
}