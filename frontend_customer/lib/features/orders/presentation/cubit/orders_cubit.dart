import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/order_model.dart';
import '../../data/repositories/orders_repository.dart';

part 'orders_state.dart';

class OrdersCubit extends Cubit<OrdersState> {
  final OrdersRepository _repository;
  OrdersCubit(this._repository) : super(OrdersInitial());

  Future<void> loadOrders({int page = 0}) async {
    emit(OrdersLoading());
    try {
      final data = await _repository.getUserOrders(page: page);
      final orders = (data['content'] as List)
          .map((e) => OrderModel.fromJson(e as Map<String, dynamic>))
          .toList();

      emit(OrdersLoaded(
        orders: orders,
        currentPage: data['number'] as int,
        totalPages: data['totalPages'] as int,
        totalElements: data['totalElements'] as int,
      ));
    } catch (e) {
      emit(OrdersError(e.toString()));
    }
  }

  Future<void> cancelOrder(int orderId) async {
    try {
      await _repository.cancelOrder(orderId);
      emit(OrderActionSuccess('Order cancelled successfully'));
      await loadOrders();
    } catch (e) {
      emit(OrdersError(e.toString()));
    }
  }
}