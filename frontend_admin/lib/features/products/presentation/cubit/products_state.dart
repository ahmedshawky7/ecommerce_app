part of 'products_cubit.dart';

abstract class ProductsState {}

class ProductsInitial extends ProductsState {}
class ProductsLoading extends ProductsState {}
class ProductsLoaded extends ProductsState {
  final List<ProductModel> products;
  final int currentPage;
  final int totalPages;
  final int totalElements;
  final int? selectedCategoryId;
  final String? keyword;

  ProductsLoaded({
    required this.products,
    required this.currentPage,
    required this.totalPages,
    required this.totalElements,
    this.selectedCategoryId,
    this.keyword,
  });
}
class ProductsError extends ProductsState {
  final String message;
  ProductsError(this.message);
}