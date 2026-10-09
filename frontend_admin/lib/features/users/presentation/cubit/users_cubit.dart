import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/user_model.dart';
import '../../data/repositories/users_repository.dart';

part 'users_state.dart';

class UsersCubit extends Cubit<UsersState> {
  final UsersRepository _repository;
  UsersCubit(this._repository) : super(UsersInitial());

  String? _role;
  String? _keyword;

  Future<void> loadUsers({int page = 0}) async {
    emit(UsersLoading());
    try {
      final data = await _repository.getUsers(
        page: page,
        size: 20,
        role: _role,
        keyword: _keyword,
      );

      final users = (data['content'] as List)
          .map((e) => UserModel.fromJson(e as Map<String, dynamic>))
          .toList();

      emit(UsersLoaded(
        users: users,
        currentPage: data['number'] as int,
        totalPages: data['totalPages'] as int,
        totalElements: data['totalElements'] as int,
        currentRole: _role,
        currentKeyword: _keyword,
      ));
    } catch (e) {
      emit(UsersError(e.toString()));
    }
  }

  Future<void> filterByRole(String? role) async {
    _role = role;
    await loadUsers(page: 0);
  }

  Future<void> searchByKeyword(String? keyword) async {
    _keyword = keyword;
    await loadUsers(page: 0);
  }

  Future<void> toggleStatus(int userId) async {
    try {
      await _repository.toggleUserStatus(userId);
      await loadUsers(page: 0);
    } catch (e) {
      emit(UsersError(e.toString()));
    }
  }
}