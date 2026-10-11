part of 'admin_products_cubit.dart';

abstract class AdminProductsState {}

class AdminProductsInitial extends AdminProductsState {}
class AdminProductsLoading extends AdminProductsState {}

class AdminProductsLoaded extends AdminProductsState {
  final List<dynamic> products;
  final int currentPage;
  final int totalPages;
  final int totalElements;
  final String? keyword;

  AdminProductsLoaded({
    required this.products,
    required this.currentPage,
    required this.totalPages,
    required this.totalElements,
    this.keyword,
  });
}

class AdminProductsError extends AdminProductsState {
  final String message;
  AdminProductsError(this.message);
}

class AdminProductActionSuccess extends AdminProductsState {
  final String message;
  AdminProductActionSuccess(this.message);
}