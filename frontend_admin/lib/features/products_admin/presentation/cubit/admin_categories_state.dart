part of 'admin_categories_cubit.dart';

abstract class AdminCategoriesState {}

class AdminCategoriesInitial extends AdminCategoriesState {}
class AdminCategoriesLoading extends AdminCategoriesState {}

class AdminCategoriesLoaded extends AdminCategoriesState {
  final List<dynamic> categories;
  AdminCategoriesLoaded(this.categories);
}

class AdminCategoriesError extends AdminCategoriesState {
  final String message;
  AdminCategoriesError(this.message);
}

class AdminCategoryActionSuccess extends AdminCategoriesState {
  final String message;
  AdminCategoryActionSuccess(this.message);
}