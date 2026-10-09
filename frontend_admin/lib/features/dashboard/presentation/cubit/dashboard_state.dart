part of 'dashboard_cubit.dart';

abstract class DashboardState {}

class DashboardInitial extends DashboardState {}
class DashboardLoading extends DashboardState {}
class DashboardLoaded extends DashboardState {
  final DashboardStatsModel stats;
  DashboardLoaded(this.stats);
}
class DashboardError extends DashboardState {
  final String message;
  DashboardError(this.message);
}