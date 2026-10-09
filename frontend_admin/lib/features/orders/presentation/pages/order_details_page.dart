import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../data/models/order_model.dart';
import '../../data/repositories/orders_repository.dart';

class OrderDetailsPage extends StatefulWidget {
  final int orderId;
  const OrderDetailsPage({super.key, required this.orderId});

  @override
  State<OrderDetailsPage> createState() => _OrderDetailsPageState();
}

class _OrderDetailsPageState extends State<OrderDetailsPage> {
  OrderModel? _order;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadOrder();
  }

  Future<void> _loadOrder() async {
    try {
      final order = await OrdersRepository().getOrderById(widget.orderId);
      setState(() {
        _order = order;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(symbol: '\$', decimalDigits: 2);
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(
        title: Text('Order Details',
            style: GoogleFonts.poppins(fontWeight: FontWeight.w600)),
        backgroundColor: const Color(0xFF6C63FF),
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Order Info
                      _buildCard(
                        title: 'Order Information',
                        children: [
                          _buildRow('Order Number', _order!.orderNumber),
                          _buildRow('Status', _order!.status),
                          _buildRow('Payment Method', _order!.paymentMethod ?? 'N/A'),
                          _buildRow('Payment Intent',
                              _order!.paymentIntentId ?? 'N/A'),
                          _buildRow('Created At',
                              dateFormat.format(_order!.createdAt)),
                        ],
                      ),
                      const SizedBox(height: 16),
                      // Customer Info
                      _buildCard(
                        title: 'Customer Information',
                        children: [
                          _buildRow('Name', _order!.customerName),
                          _buildRow('Email', _order!.customerEmail),
                          _buildRow('Phone', _order!.phone),
                        ],
                      ),
                      const SizedBox(height: 16),
                      // Shipping
                      _buildCard(
                        title: 'Shipping Address',
                        children: [_buildRow('Address', _order!.shippingAddress)],
                      ),
                      const SizedBox(height: 16),
                      // Items
                      _buildCard(
                        title: 'Order Items',
                        children: _order!.items.map((item) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 8),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(item.productName,
                                          style: GoogleFonts.poppins(
                                              fontWeight: FontWeight.w600)),
                                      Text(
                                          'Qty: ${item.quantity} × ${currencyFormat.format(item.unitPrice)}',
                                          style: TextStyle(
                                              fontSize: 12, color: Colors.grey[600])),
                                    ],
                                  ),
                                ),
                                Text(currencyFormat.format(item.subtotal),
                                    style: GoogleFonts.poppins(
                                        fontWeight: FontWeight.w600)),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
                      const SizedBox(height: 16),
                      // Total
                      Card(
                        color: const Color(0xFF6C63FF),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12)),
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('Total Amount',
                                  style: GoogleFonts.poppins(
                                      color: Colors.white,
                                      fontSize: 18,
                                      fontWeight: FontWeight.w600)),
                              Text(currencyFormat.format(_order!.totalAmount),
                                  style: GoogleFonts.poppins(
                                      color: Colors.white,
                                      fontSize: 22,
                                      fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _buildCard({required String title, required List<Widget> children}) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title,
                style:
                    GoogleFonts.poppins(fontSize: 16, fontWeight: FontWeight.w600)),
            const Divider(height: 24),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(label,
                style: GoogleFonts.poppins(
                    fontSize: 13, color: Colors.grey[600])),
          ),
          Expanded(
            child: Text(value,
                style: GoogleFonts.poppins(
                    fontSize: 14, fontWeight: FontWeight.w500)),
          ),
        ],
      ),
    );
  }
}