import 'package:dartz/dartz.dart';
import 'package:ecommerce_app/features/home_screen/cubit/categories_state.dart';
import 'package:ecommerce_app/features/home_screen/models/categories_model.dart';
import 'package:ecommerce_app/features/home_screen/repo/home_repo.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class CategoriesCubit extends Cubit<CategoriesState> {
  CategoriesCubit(this._homeRepo) : super(CategoriesInitial());
  final HomeRepo _homeRepo;
  void getCategories() async {
    emit(CategoriesLoading());
    try {
      final Either<String, List<CategoriesModel>> response = await _homeRepo
          .getCategories();
      response.fold((error) => emit(CategoriesError(error)), (categories) {
        emit(CategoriesLoaded(categories));
      });
    } catch (e) {
      emit(CategoriesError(e.toString()));
    }
  }
}
