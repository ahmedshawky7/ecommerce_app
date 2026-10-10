import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'app.dart';
import 'core/storage/secure_storage.dart';
import 'features/auth/data/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';
import 'features/cart/data/repositories/cart_repository.dart';
import 'features/cart/presentation/cubit/cart_cubit.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // ⚡ Capture URL fragment BEFORE Flutter erases it
  final uri = Uri.base;
  final fragment = uri.fragment;
  if (fragment.contains('payment-success')) {
    await SecureStorage.instance.savePaymentResult('SUCCESS');
  } else if (fragment.contains('payment-cancel')) {
    await SecureStorage.instance.savePaymentResult('CANCEL');
  }

  runApp(
    MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (_) => AuthCubit(AuthRepository())..checkAuthStatus(),
        ),
        BlocProvider(create: (_) => CartCubit(CartRepository())),
      ],
      child: const AdminApp(),
    ),
  );
}