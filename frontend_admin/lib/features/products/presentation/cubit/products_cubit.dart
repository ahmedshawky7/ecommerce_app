import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/product_model.dart';
import '../../data/repositories/products_repository.dart';

part 'products_state.dart';

class ProductsCubit extends Cubit<ProductsState> {
  final ProductsRepository _repository;
  ProductsCubit(this._repository) : super(ProductsInitial());

  int? _selectedCategoryId;
  String? _keyword;

  Future<void> loadProducts({int page = 0}) async {
    emit(ProductsLoading());
    try {
      Map<String, dynamic> data;
      if (_keyword != null && _keyword!.isNotEmpty) {
        data = await _repository.searchProducts(keyword: _keyword!, page: page);
      } else if (_selectedCategoryId != null) {
        data = await _repository.getProductsByCategory(
          categoryId: _selectedCategoryId!,
          page: page,
        );
      } else {
        data = await _repository.getProducts(page: page);
      }

      final products = (data['content'] as List)
          .map((e) => ProductModel.fromJson(e as Map<String, dynamic>))
          .toList();

      emit(ProductsLoaded(
        products: products,
        currentPage: data['number'] as int,
        totalPages: data['totalPages'] as int,
        totalElements: data['totalElements'] as int,
        selectedCategoryId: _selectedCategoryId,
        keyword: _keyword,
      ));
    } catch (e) {
      emit(ProductsError(e.toString()));
    }
  }

  Future<void> filterByCategory(int? categoryId) async {
    _selectedCategoryId = categoryId;
    _keyword = null;
    await loadProducts(page: 0);
  }

  Future<void> search(String? keyword) async {
    _keyword = keyword;
    _selectedCategoryId = null;
    await loadProducts(page: 0);
  }

  Future<void> clearFilters() async {
    _selectedCategoryId = null;
    _keyword = null;
    await loadProducts(page: 0);
  }
}