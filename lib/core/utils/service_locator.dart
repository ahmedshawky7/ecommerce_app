import 'package:ecommerce_app/core/networking/dio_helper.dart';
import 'package:ecommerce_app/core/styling/theme_cubit.dart';
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:ecommerce_app/features/auth/cubit/auth_cubit.dart';
import 'package:ecommerce_app/features/auth/repo/auth_repo.dart';
import 'package:ecommerce_app/features/cart/cubit/cart_cubit.dart';
import 'package:ecommerce_app/features/cart/repo/cart_repo.dart';
import 'package:ecommerce_app/features/home_screen/cubit/categories_cubit.dart';
import 'package:ecommerce_app/features/home_screen/cubit/product_cubit.dart';
import 'package:ecommerce_app/features/home_screen/repo/home_repo.dart';
import 'package:get_it/get_it.dart';

GetIt sl = GetIt.instance;
void setupServiceLocator() {
  DioHelper dio = DioHelper();
  sl.registerSingleton<DioHelper>(dio);

  sl.registerLazySingleton(() => StorageHelper());

  sl.registerLazySingleton(() => AuthRepo(sl()));
  sl.registerLazySingleton(() => HomeRepo(sl()));
  sl.registerLazySingleton(() => CartRepo(sl()));

  sl.registerLazySingleton(() => ThemeCubit(sl()));

  sl.registerFactory(() => AuthCubit(sl()));
  sl.registerFactory(() => ProductCubit(sl()));
  sl.registerFactory(() => CategoriesCubit(sl()));
  sl.registerFactory(() => CartCubit(sl()));
}
