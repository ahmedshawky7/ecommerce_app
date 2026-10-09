part of 'orders_cubit.dart';

abstract class OrdersState {}

class OrdersInitial extends OrdersState {}
class OrdersLoading extends OrdersState {}
class OrdersLoaded extends OrdersState {
  final List<OrderModel> orders;
  final int currentPage;
  final int totalPages;
  final int totalElements;
  final String? currentStatus;
  final String? currentKeyword;

  OrdersLoaded({
    required this.orders,
    required this.currentPage,
    required this.totalPages,
    required this.totalElements,
    this.currentStatus,
    this.currentKeyword,
  });
}
class OrdersError extends OrdersState {
  final String message;
  OrdersError(this.message);
}