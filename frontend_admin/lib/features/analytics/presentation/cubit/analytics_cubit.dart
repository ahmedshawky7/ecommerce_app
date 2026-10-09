import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/models/sales_analytics_model.dart';
import '../../data/repositories/analytics_repository.dart';

part 'analytics_state.dart';

class AnalyticsCubit extends Cubit<AnalyticsState> {
  final AnalyticsRepository _repository;
  AnalyticsCubit(this._repository) : super(AnalyticsInitial());

  int _days = 7;

  Future<void> loadAnalytics({int? days}) async {
    if (days != null) _days = days;
    emit(AnalyticsLoading());
    try {
      final dailySales = await _repository.getDailySales(days: _days);
      final revenueByCategory = await _repository.getRevenueByCategory();
      emit(AnalyticsLoaded(
        dailySales: dailySales,
        revenueByCategory: revenueByCategory,
        days: _days,
      ));
    } catch (e) {
      emit(AnalyticsError(e.toString()));
    }
  }
}