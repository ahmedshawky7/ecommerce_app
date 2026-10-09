import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../features/auth/presentation/cubit/auth_cubit.dart';
import '../../features/dashboard/presentation/pages/dashboard_page.dart';
import '../../features/orders/data/repositories/orders_repository.dart';
import '../../features/orders/presentation/cubit/orders_cubit.dart';
import '../../features/orders/presentation/pages/orders_page.dart';
import '../../features/users/data/repositories/users_repository.dart';
import '../../features/users/presentation/cubit/users_cubit.dart';
import '../../features/users/presentation/pages/users_page.dart';
import '../../features/analytics/data/repositories/analytics_repository.dart';
import '../../features/analytics/presentation/cubit/analytics_cubit.dart';
import '../../features/analytics/presentation/pages/analytics_page.dart';
class MainLayout extends StatefulWidget {
  const MainLayout({super.key});

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  int _selectedIndex = 0;


  final _titles = const ['Dashboard', 'Orders', 'Users', 'Analytics'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          // Sidebar
          Material(
            color: const Color(0xFF1A1A2E), // ← هنا بنحدد اللون
            child: SizedBox(
              width: 240,
              child: Column(
                children: [
                  const SizedBox(height: 24),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Row(
                      children: [
                        const Icon(Icons.shopping_bag,
                            color: Color(0xFF6C63FF), size: 32),
                        const SizedBox(width: 12),
                        Text(
                          'Shop Admin',
                          style: GoogleFonts.poppins(
                              color: Colors.white,
                              fontSize: 18,
                              fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 40),
                  _buildMenuItem(0, Icons.dashboard_outlined, 'Dashboard'),
                  _buildMenuItem(1, Icons.receipt_long_outlined, 'Orders'),
                  _buildMenuItem(2, Icons.people_outline, 'Users'),
                  _buildMenuItem(3, Icons.analytics_outlined, 'Analytics'),
                  const Spacer(),
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: ListTile(
                      leading: const Icon(Icons.logout, color: Colors.redAccent),
                      title: Text('Logout',
                          style: GoogleFonts.poppins(color: Colors.redAccent)),
                      onTap: () => context.read<AuthCubit>().logout(),
                    ),
                  ),
                ],
              ),
            ),
          ),
          // Main Content
          Expanded(
            child: Column(
              children: [
                // AppBar
                Container(
                  height: 70,
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    border: Border(bottom: BorderSide(color: Colors.black12)),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Row(
                    children: [
                      Text(
                        _titles[_selectedIndex],
                        style: GoogleFonts.poppins(
                            fontSize: 22, fontWeight: FontWeight.w600),
                      ),
                      const Spacer(),
                      const CircleAvatar(
                        backgroundColor: Color(0xFF6C63FF),
                        child: Icon(Icons.person, color: Colors.white),
                      ),
                    ],
                  ),
                ),
                // Page Content
                 Expanded(child: _buildPage()),
              ],
            ),
          ),
        ],
      ),
   
    );
  }

  Widget _buildPage() {
    switch (_selectedIndex) {
      case 0:
        return const DashboardPage();
      case 1:
        return BlocProvider(
          create: (_) => OrdersCubit(OrdersRepository()),
          child: const OrdersPage(),
        );
      case 2:
        return BlocProvider(
          create: (_) => UsersCubit(UsersRepository()),
          child: const UsersPage(),
        );
      case 3:
        return BlocProvider(
          create: (_) => AnalyticsCubit(AnalyticsRepository()),
          child: const AnalyticsPage(),
        );
      default:
        return const DashboardPage();
    }
  }

  Widget _buildMenuItem(int index, IconData icon, String label) {
    final isSelected = _selectedIndex == index;
    return Material(
      color: isSelected 
          ? const Color(0xFF6C63FF).withOpacity(0.2) 
          : Colors.transparent,
      child: ListTile(
        leading: Icon(icon,
            color: isSelected ? Colors.white : Colors.white54, size: 22),
        title: Text(
          label,
          style: GoogleFonts.poppins(
            color: isSelected ? Colors.white : Colors.white54,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
            fontSize: 14,
          ),
        ),
        onTap: () => setState(() => _selectedIndex = index),
      ),
    );
  }
}