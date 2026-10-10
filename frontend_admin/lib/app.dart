import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/main_layout.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';
import 'features/auth/presentation/pages/login_page.dart';
import 'features/cart/data/repositories/cart_repository.dart';
import 'features/cart/presentation/cubit/cart_cubit.dart';
import 'features/dashboard/data/repositories/dashboard_repository.dart';
import 'features/dashboard/presentation/cubit/dashboard_cubit.dart';
import 'features/products/presentation/pages/home_page.dart';

class AdminApp extends StatelessWidget {
  const AdminApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'E-Commerce',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: BlocBuilder<AuthCubit, AuthState>(
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
      ),
    );
  }
}
