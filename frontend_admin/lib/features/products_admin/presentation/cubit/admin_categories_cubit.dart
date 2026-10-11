import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../products/data/models/category_model.dart';
import '../../data/repositories/admin_categories_repository.dart';

part 'admin_categories_state.dart';

class AdminCategoriesCubit extends Cubit<AdminCategoriesState> {
  final AdminCategoriesRepository _repository;
  AdminCategoriesCubit(this._repository) : super(AdminCategoriesInitial());

  Future<void> loadCategories() async {
    emit(AdminCategoriesLoading());
    try {
      final categories = await _repository.getCategories();
      emit(AdminCategoriesLoaded(categories));
    } catch (e) {
      emit(AdminCategoriesError(e.toString()));
    }
  }

  Future<void> createCategory({
    required String name,
    String? description,
    String? imageUrl,
  }) async {
    try {
      await _repository.createCategory(
        name: name,
        description: description,
        imageUrl: imageUrl,
      );
      emit(AdminCategoryActionSuccess('Category created successfully'));
      await loadCategories();
    } catch (e) {
      emit(AdminCategoriesError(e.toString()));
    }
  }

  Future<void> updateCategory({
    required int id,
    required String name,
    String? description,
    String? imageUrl,
  }) async {
    try {
      await _repository.updateCategory(
        id: id,
        name: name,
        description: description,
        imageUrl: imageUrl,
      );
      emit(AdminCategoryActionSuccess('Category updated successfully'));
      await loadCategories();
    } catch (e) {
      emit(AdminCategoriesError(e.toString()));
    }
  }

  Future<void> deleteCategory(int id) async {
    try {
      await _repository.deleteCategory(id);
      emit(AdminCategoryActionSuccess('Category deleted successfully'));
      await loadCategories();
    } catch (e) {
      emit(AdminCategoriesError(e.toString()));
    }
  }
}