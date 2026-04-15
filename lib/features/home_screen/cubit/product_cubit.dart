import 'package:ecommerce_app/features/home_screen/cubit/product_state.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';
import 'package:ecommerce_app/features/home_screen/repo/home_repo.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:dartz/dartz.dart';

class ProductCubit extends Cubit<ProductState> {
  final HomeRepo homeRepo; // الـ repository هنا
  ProductCubit(this.homeRepo) : super(ProductInitial());

  Future<void> getAllProducts() async {
    emit(ProductLoading());
    final Either<String, List<Product>> response = await homeRepo.getProducts();
    response.fold((error) => emit(ProductError(error)), (data) {
      emit(ProductLoaded(data));
    });
  }

  // جلب المنتجات حسب الكاتيجوري
  Future<void> getProductsByCategory(String category) async {
    emit(ProductLoading());
    final Either<String, List<Product>> response = await homeRepo
        .getProductsCategories(category);
    response.fold((error) => emit(ProductError(error)), (data) {
      emit(ProductLoaded(data));
    });
  }
}
