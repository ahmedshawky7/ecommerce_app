import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../cubit/analytics_cubit.dart';

class AnalyticsPage extends StatefulWidget {
  const AnalyticsPage({super.key});

  @override
  State<AnalyticsPage> createState() => _AnalyticsPageState();
}

class _AnalyticsPageState extends State<AnalyticsPage> {
  @override
  void initState() {
    super.initState();
    context.read<AnalyticsCubit>().loadAnalytics();
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 2);

    return BlocBuilder<AnalyticsCubit, AnalyticsState>(
      builder: (context, state) {
        if (state is AnalyticsLoading || state is AnalyticsInitial) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state is AnalyticsError) {
          return Center(child: Text(state.message));
        }
        if (state is AnalyticsLoaded) {
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header with days filter
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Sales Overview',
                        style: GoogleFonts.poppins(
                            fontSize: 20, fontWeight: FontWeight.w600)),
                    DropdownButton<int>(
                      value: state.days,
                      items: const [
                        DropdownMenuItem(value: 7, child: Text('Last 7 days')),
                        DropdownMenuItem(value: 30, child: Text('Last 30 days')),
                        DropdownMenuItem(value: 90, child: Text('Last 90 days')),
                      ],
                      onChanged: (value) {
                        if (value != null) {
                          context.read<AnalyticsCubit>().loadAnalytics(days: value);
                        }
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                // Sales Chart
                Card(
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Daily Sales',
                            style: GoogleFonts.poppins(
                                fontSize: 16, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 20),
                        SizedBox(
                          height: 280,
                          child: state.dailySales.isEmpty
                              ? Center(
                                  child: Text('No data available',
                                      style: GoogleFonts.poppins()))
                              : LineChart(
                                  LineChartData(
                                    gridData: const FlGridData(show: true),
                                    titlesData: FlTitlesData(
                                      leftTitles: AxisTitles(
                                        sideTitles: SideTitles(
                                          showTitles: true,
                                          reservedSize: 60,
                                          getTitlesWidget: (value, meta) {
                                            return Text(
                                              '\$${value.toInt()}',
                                              style: const TextStyle(fontSize: 10),
                                            );
                                          },
                                        ),
                                      ),
                                      bottomTitles: AxisTitles(
                                        sideTitles: SideTitles(
                                          showTitles: true,
                                          getTitlesWidget: (value, meta) {
                                            final index = value.toInt();
                                            if (index < 0 ||
                                                index >= state.dailySales.length) {
                                              return const SizedBox();
                                            }
                                            final date = state.dailySales[index].date;
                                            return Padding(
                                              padding:
                                                  const EdgeInsets.only(top: 8),
                                              child: Text(
                                                date.substring(5),
                                                style:
                                                    const TextStyle(fontSize: 10),
                                              ),
                                            );
                                          },
                                        ),
                                      ),
                                      topTitles: const AxisTitles(
                                          sideTitles: SideTitles(showTitles: false)),
                                      rightTitles: const AxisTitles(
                                          sideTitles: SideTitles(showTitles: false)),
                                    ),
                                    borderData: FlBorderData(show: false),
                                    lineBarsData: [
                                      LineChartBarData(
                                        spots: state.dailySales
                                            .asMap()
                                            .entries
                                            .map((e) => FlSpot(
                                                  e.key.toDouble(),
                                                  e.value.revenue,
                                                ))
                                            .toList(),
                                        isCurved: true,
                                        color: const Color(0xFF6C63FF),
                                        barWidth: 3,
                                        dotData: const FlDotData(show: true),
                                        belowBarData: BarAreaData(
                                          show: true,
                                          color: const Color(0xFF6C63FF)
                                              .withOpacity(0.15),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                // Revenue by Category
                Card(
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Revenue by Category',
                            style: GoogleFonts.poppins(
                                fontSize: 16, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 20),
                        if (state.revenueByCategory.isEmpty)
                          Center(
                            child: Padding(
                              padding: const EdgeInsets.all(20),
                              child: Text('No data available',
                                  style: GoogleFonts.poppins()),
                            ),
                          )
                        else
                          ...state.revenueByCategory.map((cat) {
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 8),
                              child: Row(
                                children: [
                                  Expanded(
                                    flex: 2,
                                    child: Text(cat.categoryName,
                                        style: GoogleFonts.poppins(
                                            fontWeight: FontWeight.w500)),
                                  ),
                                  Expanded(
                                    flex: 3,
                                    child: LinearProgressIndicator(
                                      value: cat.totalRevenue /
                                          state.revenueByCategory
                                              .map((e) => e.totalRevenue)
                                              .reduce((a, b) => a + b),
                                      backgroundColor: Colors.grey[200],
                                      color: const Color(0xFF6C63FF),
                                      minHeight: 8,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  SizedBox(
                                    width: 100,
                                    child: Text(
                                      currencyFormat.format(cat.totalRevenue),
                                      textAlign: TextAlign.end,
                                      style: GoogleFonts.poppins(
                                          fontWeight: FontWeight.w600),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          }),
                      ],
                    ),
                  ),
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