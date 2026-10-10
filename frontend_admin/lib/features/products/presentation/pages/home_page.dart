import 'package:badges/badges.dart' as badges;
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../../cart/presentation/cubit/cart_cubit.dart';
import '../../../cart/presentation/pages/cart_page.dart';
import '../../data/repositories/categories_repository.dart';
import '../../data/repositories/products_repository.dart';
import '../cubit/categories_cubit.dart';
import '../cubit/products_cubit.dart';
import 'products_page.dart';
import '../../../checkout/presentation/pages/payment_success_page.dart';
import '../../../checkout/presentation/pages/payment_cancel_page.dart';
import '../../../orders/data/repositories/orders_repository.dart';
import '../../../orders/presentation/cubit/orders_cubit.dart';
import '../../../orders/presentation/pages/orders_page.dart';
import '../../../profile/presentation/pages/profile_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _selectedIndex = 0;

  @override
  void initState() {
    super.initState();
    context.read<CartCubit>().loadCart();

    // Check return URL from Stripe
    final uri = Uri.base;
    if (uri.fragment.contains('payment-success')) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        Navigator.of(
          context,
        ).push(MaterialPageRoute(builder: (_) => const PaymentSuccessPage()));
      });
    } else if (uri.fragment.contains('payment-cancel')) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        Navigator.of(context)
            .push(MaterialPageRoute(builder: (_) => const PaymentCancelPage()));
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(create: (_) => ProductsCubit(ProductsRepository())),
        BlocProvider(create: (_) => CategoriesCubit(CategoriesRepository())),
      ],
      child: Scaffold(
        appBar: (_selectedIndex == 0 || _selectedIndex == 2)
            ? AppBar(
                title: Text(
                  _selectedIndex == 0 ? 'Shop' : 'My Orders',
                  style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
                ),
                actions: [
                  IconButton(
                    icon: const Icon(Icons.notifications_outlined),
                    onPressed: () {},
                  ),
                ],
              )
            : null,
        body: _buildPage(),
        bottomNavigationBar: _buildBottomNav(),
      ),
    );
  }

  Widget _buildBottomNav() {
    return BlocBuilder<CartCubit, CartState>(
      builder: (context, state) {
        int cartCount = 0;
        if (state is CartLoaded) {
          cartCount = state.cart.totalItems;
        }

        return BottomNavigationBar(
          currentIndex: _selectedIndex,
          onTap: (index) => setState(() => _selectedIndex = index),
          type: BottomNavigationBarType.fixed,
          selectedItemColor: const Color(0xFF6C63FF),
          unselectedItemColor: Colors.grey,
          items: [
            const BottomNavigationBarItem(
              icon: Icon(Icons.home_outlined),
              activeIcon: Icon(Icons.home),
              label: 'Home',
            ),
            BottomNavigationBarItem(
              icon: cartCount > 0
                  ? badges.Badge(
                      badgeContent: Text(
                        cartCount.toString(),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                        ),
                      ),
                      badgeStyle: const badges.BadgeStyle(
                        badgeColor: Color(0xFFFF6584),
                      ),
                      child: const Icon(Icons.shopping_cart_outlined),
                    )
                  : const Icon(Icons.shopping_cart_outlined),
              activeIcon: cartCount > 0
                  ? badges.Badge(
                      badgeContent: Text(
                        cartCount.toString(),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                        ),
                      ),
                      badgeStyle: const badges.BadgeStyle(
                        badgeColor: Color(0xFFFF6584),
                      ),
                      child: const Icon(Icons.shopping_cart),
                    )
                  : const Icon(Icons.shopping_cart),
              label: 'Cart',
            ),
            const BottomNavigationBarItem(
              icon: Icon(Icons.receipt_long_outlined),
              activeIcon: Icon(Icons.receipt_long),
              label: 'Orders',
            ),
            const BottomNavigationBarItem(
              icon: Icon(Icons.person_outline),
              activeIcon: Icon(Icons.person),
              label: 'Profile',
            ),
          ],
        );
      },
    );
  }

  Widget _buildPage() {
    switch (_selectedIndex) {
      case 0:
        return const ProductsPage();
      case 1:
        return const CartPage();
      case 2:
        return BlocProvider(
          create: (_) => OrdersCubit(OrdersRepository()),
          child: const OrdersPage(),
        );
      case 3:
        return const ProfilePage();
      default:
        return const ProductsPage();
    }
  }
}
