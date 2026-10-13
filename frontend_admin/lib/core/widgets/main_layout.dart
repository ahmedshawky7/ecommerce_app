import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../features/analytics/data/repositories/analytics_repository.dart';
import '../../features/analytics/presentation/cubit/analytics_cubit.dart';
import '../../features/analytics/presentation/pages/analytics_page.dart';
import '../../features/auth/presentation/cubit/auth_cubit.dart';
import '../../features/dashboard/data/repositories/dashboard_repository.dart';
import '../../features/dashboard/presentation/cubit/dashboard_cubit.dart';
import '../../features/dashboard/presentation/pages/dashboard_page.dart';
import '../../features/orders/data/repositories/orders_repository.dart';
import '../../features/orders/presentation/cubit/orders_cubit.dart';
import '../../features/orders/presentation/pages/orders_page.dart';
import '../../features/products_admin/data/repositories/admin_categories_repository.dart';
import '../../features/products_admin/data/repositories/admin_products_repository.dart';
import '../../features/products_admin/presentation/cubit/admin_categories_cubit.dart';
import '../../features/products_admin/presentation/cubit/admin_products_cubit.dart';
import '../../features/products_admin/presentation/pages/categories_admin_page.dart';
import '../../features/products_admin/presentation/pages/products_admin_page.dart';
import '../../features/users/data/repositories/users_repository.dart';
import '../../features/users/presentation/cubit/users_cubit.dart';
import '../../features/users/presentation/pages/users_page.dart';

class MainLayout extends StatefulWidget {
  const MainLayout({super.key});

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  int _selectedIndex = 0;
  final _scaffoldKey = GlobalKey<ScaffoldState>();

  static const _titles = [
    'Dashboard',
    'Orders',
    'Users',
    'Products',
    'Categories',
    'Analytics',
  ];

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        // Breakpoint: 900px
        final isMobile = constraints.maxWidth < 900;

        return Scaffold(
          key: _scaffoldKey,
          backgroundColor: const Color(0xFFF5F5F7),
          drawer: isMobile ? Drawer(child: _buildSidebar(isMobile: true)) : null,
          body: Row(
            children: [
              if (!isMobile) _buildSidebar(isMobile: false),
              Expanded(
                child: Column(
                  children: [
                    _buildAppBar(isMobile),
                    Expanded(child: _buildPage()),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildAppBar(bool isMobile) {
    return Container(
      height: 70,
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: Colors.black12)),
      ),
      padding: EdgeInsets.symmetric(horizontal: isMobile ? 12 : 24),
      child: Row(
        children: [
          if (isMobile)
            IconButton(
              icon: const Icon(Icons.menu, size: 24),
              onPressed: () => _scaffoldKey.currentState?.openDrawer(),
            ),
          if (isMobile) const SizedBox(width: 4),
          Text(
            _titles[_selectedIndex],
            style: GoogleFonts.poppins(
              fontSize: isMobile ? 18 : 22,
              fontWeight: FontWeight.w600,
            ),
          ),
          const Spacer(),
          const CircleAvatar(
            backgroundColor: Color(0xFF6C63FF),
            child: Icon(Icons.person, color: Colors.white),
          ),
        ],
      ),
    );
  }

  Widget _buildSidebar({required bool isMobile}) {
    return Material(
      color: const Color(0xFF1A1A2E),
      child: SizedBox(
        width: isMobile ? null : 240,
        child: SafeArea(
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
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 40),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      _buildMenuItem(0, Icons.dashboard_outlined, 'Dashboard',
                          isMobile: isMobile),
                      _buildMenuItem(1, Icons.receipt_long_outlined, 'Orders',
                          isMobile: isMobile),
                      _buildMenuItem(2, Icons.people_outline, 'Users',
                          isMobile: isMobile),
                      _buildMenuItem(3, Icons.inventory_2_outlined, 'Products',
                          isMobile: isMobile),
                      _buildMenuItem(4, Icons.category_outlined, 'Categories',
                          isMobile: isMobile),
                      _buildMenuItem(5, Icons.analytics_outlined, 'Analytics',
                          isMobile: isMobile),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(20),
                child: ListTile(
                  leading:
                      const Icon(Icons.logout, color: Colors.redAccent),
                  title: Text('Logout',
                      style: GoogleFonts.poppins(color: Colors.redAccent)),
                  onTap: () => context.read<AuthCubit>().logout(),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMenuItem(int index, IconData icon, String label,
      {bool isMobile = false}) {
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
        onTap: () {
          setState(() => _selectedIndex = index);
          if (isMobile) Navigator.pop(context);
        },
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
          create: (_) => AdminProductsCubit(AdminProductsRepository()),
          child: const ProductsAdminPage(),
        );
      case 4:
        return BlocProvider(
          create: (_) => AdminCategoriesCubit(AdminCategoriesRepository()),
          child: const CategoriesAdminPage(),
        );
      case 5:
        return BlocProvider(
          create: (_) => AnalyticsCubit(AnalyticsRepository()),
          child: const AnalyticsPage(),
        );
      default:
        return const DashboardPage();
    }
  }
}