part of 'analytics_cubit.dart';

abstract class AnalyticsState {}

class AnalyticsInitial extends AnalyticsState {}
class AnalyticsLoading extends AnalyticsState {}
class AnalyticsLoaded extends AnalyticsState {
  final List<SalesAnalyticsModel> dailySales;
  final List<RevenueByCategoryModel> revenueByCategory;
  final int days;

  AnalyticsLoaded({
    required this.dailySales,
    required this.revenueByCategory,
    required this.days,
  });
}
class AnalyticsError extends AnalyticsState {
  final String message;
  AnalyticsError(this.message);
}