import 'package:dartz/dartz.dart';
import 'package:ecommerce_app/core/networking/api_endpoints.dart';
import 'package:ecommerce_app/core/networking/dio_helper.dart';
import 'package:ecommerce_app/features/home_screen/models/categories_model.dart';
import 'package:ecommerce_app/features/home_screen/models/products_model.dart';

class HomeRepo {
  final DioHelper _dioHelper;
  HomeRepo(this._dioHelper);
  Future<Either<String, List<Product>>> getProducts() async {
    try {
      final response = await _dioHelper.getRequest(
        endPoint: ApiEndpoints.products,
      );

      if (response.statusCode == 200) {
        ProductsModel productsModel = ProductsModel.fromJson(response.data);

        return Right(productsModel.products);
      } else {
        return Left("Something went wrong");
      }
    } catch (e) {
      return Left(e.toString());
    }
  }

  Future<Either<String, List<Product>>> getProductsCategories(
    String categoryName,
  ) async {
    try {
      final response = await _dioHelper.getRequest(
        endPoint:
            '${ApiEndpoints.products}/${ApiEndpoints.catProducts}/$categoryName',
      );

      if (response.statusCode == 200) {
        ProductsModel productsModel = ProductsModel.fromJson(response.data);

        return Right(productsModel.products);
      } else {
        return Left("Something went wrong");
      }
    } catch (e) {
      return Left(e.toString());
    }
  }

  Future<Either<String, List<CategoriesModel>>> getCategories() async {
    try {
      final response = await _dioHelper.getRequest(
        endPoint: ApiEndpoints.categories,
      );

      if (response.statusCode == 200) {
        List<CategoriesModel> categories = (response.data as List)
            .map((e) => CategoriesModel.fromJson(e))
            .toList();
        categories.insert(0, CategoriesModel(slug: 'All', name: 'All', url: ''));

        return Right(categories);
      } else {
        return Left("Something went wrong");
      }
    } catch (e) {
      return Left(e.toString());
    }
  }
}
