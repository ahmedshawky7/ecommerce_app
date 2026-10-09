import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../../../core/widgets/stat_card.dart';
import '../cubit/dashboard_cubit.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  @override
  void initState() {
    super.initState();
    context.read<DashboardCubit>().loadStats();
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<DashboardCubit, DashboardState>(
      builder: (context, state) {
        if (state is DashboardLoading || state is DashboardInitial) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state is DashboardError) {
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, color: Colors.red, size: 48),
                const SizedBox(height: 12),
                Text(state.message),
                const SizedBox(height: 12),
                ElevatedButton(
                  onPressed: () => context.read<DashboardCubit>().loadStats(),
                  child: const Text('Retry'),
                ),
              ],
            ),
          );
        }
        if (state is DashboardLoaded) {
          final stats = state.stats;
          final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 2);

          return SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Overview',
                    style: GoogleFonts.poppins(
                        fontSize: 20, fontWeight: FontWeight.w600)),
                const SizedBox(height: 16),
                // Stats Grid
                GridView.count(
                  crossAxisCount: 4,
                  crossAxisSpacing: 16,
                  mainAxisSpacing: 16,
                  childAspectRatio: 2.2,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  children: [
                    StatCard(
                      title: 'Total Revenue',
                      value: currencyFormat.format(stats.totalRevenue),
                      icon: Icons.attach_money,
                      color: const Color(0xFF4CAF50),
                    ),
                    StatCard(
                      title: 'Total Orders',
                      value: stats.totalOrders.toString(),
                      icon: Icons.shopping_cart_outlined,
                      color: const Color(0xFF2196F3),
                    ),
                    StatCard(
                      title: 'Customers',
                      value: stats.totalCustomers.toString(),
                      icon: Icons.people_outline,
                      color: const Color(0xFF9C27B0),
                    ),
                    StatCard(
                      title: 'Products',
                      value: stats.totalProducts.toString(),
                      icon: Icons.inventory_2_outlined,
                      color: const Color(0xFFFF9800),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                // Order Status
                Text('Orders by Status',
                    style: GoogleFonts.poppins(
                        fontSize: 18, fontWeight: FontWeight.w600)),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: _StatusCard(
                        label: 'Pending',
                        count: stats.pendingOrders,
                        color: const Color(0xFFFF9800),
                        icon: Icons.pending_outlined,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: _StatusCard(
                        label: 'Confirmed',
                        count: stats.confirmedOrders,
                        color: const Color(0xFF4CAF50),
                        icon: Icons.check_circle_outline,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: _StatusCard(
                        label: 'Cancelled',
                        count: stats.cancelledOrders,
                        color: const Color(0xFFF44336),
                        icon: Icons.cancel_outlined,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          );
        }
        return const SizedBox.shrink();
      },
    );
  }
}

class _StatusCard extends StatelessWidget {
  final String label;
  final int count;
  final Color color;
  final IconData icon;

  const _StatusCard({
    required this.label,
    required this.count,
    required this.color,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: color, size: 32),
            const SizedBox(height: 12),
            Text(
              count.toString(),
              style: GoogleFonts.poppins(
                  fontSize: 28, fontWeight: FontWeight.bold),
            ),
            Text(
              label,
              style: GoogleFonts.poppins(color: Colors.grey[600], fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}