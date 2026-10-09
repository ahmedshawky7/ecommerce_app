import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/dashboard_stats_model.dart';
import '../../data/repositories/dashboard_repository.dart';

part 'dashboard_state.dart';

class DashboardCubit extends Cubit<DashboardState> {
  final DashboardRepository _repository;
  DashboardCubit(this._repository) : super(DashboardInitial());

  Future<void> loadStats() async {
    emit(DashboardLoading());
    try {
      final stats = await _repository.getStats();
      emit(DashboardLoaded(stats));
    } catch (e) {
      emit(DashboardError(e.toString()));
    }
  }
}