import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/order_model.dart';
import '../../data/repositories/orders_repository.dart';

part 'orders_state.dart';

class OrdersCubit extends Cubit<OrdersState> {
  final OrdersRepository _repository;
  OrdersCubit(this._repository) : super(OrdersInitial());

  int _currentPage = 0;
  static const int _pageSize = 20;
  String? _status;
  String? _keyword;

  Future<void> loadOrders({int page = 0}) async {
    emit(OrdersLoading());
    try {
      _currentPage = page;
      final data = await _repository.getOrders(
        page: page,
        size: _pageSize,
        status: _status,
        keyword: _keyword,
      );

      final orders = (data['content'] as List)
          .map((e) => OrderModel.fromJson(e as Map<String, dynamic>))
          .toList();

      emit(OrdersLoaded(
        orders: orders,
        currentPage: data['number'] as int,
        totalPages: data['totalPages'] as int,
        totalElements: data['totalElements'] as int,
        currentStatus: _status,
        currentKeyword: _keyword,
      ));
    } catch (e) {
      emit(OrdersError(e.toString()));
    }
  }

  Future<void> filterByStatus(String? status) async {
    _status = status;
    await loadOrders(page: 0);
  }

  Future<void> searchByKeyword(String? keyword) async {
    _keyword = keyword;
    await loadOrders(page: 0);
  }

  Future<void> clearFilters() async {
    _status = null;
    _keyword = null;
    await loadOrders(page: 0);
  }
}