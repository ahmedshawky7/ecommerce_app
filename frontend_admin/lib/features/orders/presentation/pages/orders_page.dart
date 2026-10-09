import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../cubit/orders_cubit.dart';
import 'order_details_page.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    context.read<OrdersCubit>().loadOrders();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'PENDING':
        return const Color(0xFFFF9800);
      case 'CONFIRMED':
        return const Color(0xFF2196F3);
      case 'PROCESSING':
        return const Color(0xFF9C27B0);
      case 'SHIPPED':
        return const Color(0xFF00BCD4);
      case 'DELIVERED':
        return const Color(0xFF4CAF50);
      case 'CANCELLED':
        return const Color(0xFFF44336);
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 2);
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return BlocBuilder<OrdersCubit, OrdersState>(
      builder: (context, state) {
        return Column(
          children: [
            // Filter Bar
            Padding(
              padding: const EdgeInsets.all(24),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _searchController,
                      decoration: InputDecoration(
                        hintText: 'Search by order number...',
                        prefixIcon: const Icon(Icons.search),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        suffixIcon: IconButton(
                          icon: const Icon(Icons.clear),
                          onPressed: () {
                            _searchController.clear();
                            context.read<OrdersCubit>().clearFilters();
                          },
                        ),
                      ),
                      onSubmitted: (value) => context
                          .read<OrdersCubit>()
                          .searchByKeyword(value.trim()),
                    ),
                  ),
                  const SizedBox(width: 16),
                  DropdownButton<String?>(
                    value: state is OrdersLoaded ? state.currentStatus : null,
                    hint: const Text('Filter by status'),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('All')),
                      DropdownMenuItem(value: 'PENDING', child: Text('Pending')),
                      DropdownMenuItem(value: 'CONFIRMED', child: Text('Confirmed')),
                      DropdownMenuItem(value: 'PROCESSING', child: Text('Processing')),
                      DropdownMenuItem(value: 'SHIPPED', child: Text('Shipped')),
                      DropdownMenuItem(value: 'DELIVERED', child: Text('Delivered')),
                      DropdownMenuItem(value: 'CANCELLED', child: Text('Cancelled')),
                    ],
                    onChanged: (value) =>
                        context.read<OrdersCubit>().filterByStatus(value),
                  ),
                ],
              ),
            ),
            // Table
            Expanded(
              child: _buildTable(context, state, currencyFormat, dateFormat),
            ),
          ],
        );
      },
    );
  }

  Widget _buildTable(
    BuildContext context,
    OrdersState state,
    NumberFormat currencyFormat,
    DateFormat dateFormat,
  ) {
    if (state is OrdersLoading || state is OrdersInitial) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state is OrdersError) {
      return Center(child: Text(state.message));
    }
    if (state is OrdersLoaded) {
      if (state.orders.isEmpty) {
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.inbox, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 12),
              Text('No orders found', style: GoogleFonts.poppins()),
            ],
          ),
        );
      }

      return SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: Column(
            children: [
              DataTable(
                headingRowColor:
                    MaterialStateProperty.all(const Color(0xFFF5F5F7)),
                columns: const [
                  DataColumn(label: Text('#')),
                  DataColumn(label: Text('Order #')),
                  DataColumn(label: Text('Customer')),
                  DataColumn(label: Text('Total')),
                  DataColumn(label: Text('Status')),
                  DataColumn(label: Text('Date')),
                  DataColumn(label: Text('Actions')),
                ],
                rows: state.orders.map((order) {
                  return DataRow(cells: [
                    DataCell(Text(order.id.toString())),
                    DataCell(Text(order.orderNumber,
                        style: const TextStyle(fontWeight: FontWeight.w600))),
                    DataCell(Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(order.customerName, style: const TextStyle(fontSize: 13)),
                        Text(order.customerEmail,
                            style: TextStyle(fontSize: 11, color: Colors.grey[600])),
                      ],
                    )),
                    DataCell(Text(currencyFormat.format(order.totalAmount),
                        style: const TextStyle(fontWeight: FontWeight.w600))),
                    DataCell(Container(
                      padding:
                          const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: _getStatusColor(order.status).withOpacity(0.15),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        order.status,
                        style: TextStyle(
                          color: _getStatusColor(order.status),
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    )),
                    DataCell(Text(dateFormat.format(order.createdAt))),
                    DataCell(IconButton(
                      icon: const Icon(Icons.visibility_outlined, size: 20),
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => OrderDetailsPage(orderId: order.id),
                          ),
                        );
                      },
                    )),
                  ]);
                }).toList(),
              ),
              // Pagination
              Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Page ${state.currentPage + 1} of ${state.totalPages} '
                      '(${state.totalElements} orders)',
                      style: GoogleFonts.poppins(fontSize: 13),
                    ),
                    Row(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.chevron_left),
                          onPressed: state.currentPage > 0
                              ? () => context
                                  .read<OrdersCubit>()
                                  .loadOrders(page: state.currentPage - 1)
                              : null,
                        ),
                        IconButton(
                          icon: const Icon(Icons.chevron_right),
                          onPressed: state.currentPage < state.totalPages - 1
                              ? () => context
                                  .read<OrdersCubit>()
                                  .loadOrders(page: state.currentPage + 1)
                              : null,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
    return const SizedBox.shrink();
  }
}