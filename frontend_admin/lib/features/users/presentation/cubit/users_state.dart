part of 'users_cubit.dart';

abstract class UsersState {}

class UsersInitial extends UsersState {}
class UsersLoading extends UsersState {}
class UsersLoaded extends UsersState {
  final List<UserModel> users;
  final int currentPage;
  final int totalPages;
  final int totalElements;
  final String? currentRole;
  final String? currentKeyword;

  UsersLoaded({
    required this.users,
    required this.currentPage,
    required this.totalPages,
    required this.totalElements,
    this.currentRole,
    this.currentKeyword,
  });
}
class UsersError extends UsersState {
  final String message;
  UsersError(this.message);
}