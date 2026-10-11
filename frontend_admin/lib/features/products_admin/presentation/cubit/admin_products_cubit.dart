import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../products/data/models/product_model.dart';
import '../../data/repositories/admin_products_repository.dart';

part 'admin_products_state.dart';

class AdminProductsCubit extends Cubit<AdminProductsState> {
  final AdminProductsRepository _repository;
  AdminProductsCubit(this._repository) : super(AdminProductsInitial());

  String? _keyword;

  Future<void> loadProducts({int page = 0}) async {
    emit(AdminProductsLoading());
    try {
      final data = await _repository.getProducts(
        page: page,
        keyword: _keyword,
      );

      final products = (data['content'] as List)
          .map((e) => ProductModel.fromJson(e as Map<String, dynamic>))
          .toList();

      emit(AdminProductsLoaded(
        products: products,
        currentPage: data['number'] as int,
        totalPages: data['totalPages'] as int,
        totalElements: data['totalElements'] as int,
        keyword: _keyword,
      ));
    } catch (e) {
      emit(AdminProductsError(e.toString()));
    }
  }

  Future<void> search(String? keyword) async {
    _keyword = keyword;
    await loadProducts(page: 0);
  }

  Future<void> createProduct({
    required String name,
    required String description,
    required double price,
    required int stockQuantity,
    String? imageUrl,
    required int categoryId,
  }) async {
    try {
      await _repository.createProduct(
        name: name,
        description: description,
        price: price,
        stockQuantity: stockQuantity,
        imageUrl: imageUrl,
        categoryId: categoryId,
      );
      emit(AdminProductActionSuccess('Product created successfully'));
      await loadProducts();
    } catch (e) {
      emit(AdminProductsError(e.toString()));
    }
  }

  Future<void> updateProduct({
    required int id,
    required String name,
    required String description,
    required double price,
    required int stockQuantity,
    String? imageUrl,
    required int categoryId,
  }) async {
    try {
      await _repository.updateProduct(
        id: id,
        name: name,
        description: description,
        price: price,
        stockQuantity: stockQuantity,
        imageUrl: imageUrl,
        categoryId: categoryId,
      );
      emit(AdminProductActionSuccess('Product updated successfully'));
      await loadProducts();
    } catch (e) {
      emit(AdminProductsError(e.toString()));
    }
  }

  Future<void> deleteProduct(int id) async {
    try {
      await _repository.deleteProduct(id);
      emit(AdminProductActionSuccess('Product deleted successfully'));
      await loadProducts();
    } catch (e) {
      emit(AdminProductsError(e.toString()));
    }
  }
}