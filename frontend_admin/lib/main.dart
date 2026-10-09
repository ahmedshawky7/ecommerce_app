import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'app.dart';
import 'features/auth/data/repositories/auth_repository.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';

void main() {
  runApp(
    BlocProvider(
      create: (_) => AuthCubit(AuthRepository()),
      child: const AdminApp(),
    ),
  );
}