import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../cubit/admin_products_cubit.dart';
import 'product_form_page.dart';
import '../../data/repositories/admin_categories_repository.dart';
import '../cubit/admin_categories_cubit.dart';

class ProductsAdminPage extends StatefulWidget {
  const ProductsAdminPage({super.key});

  @override
  State<ProductsAdminPage> createState() => _ProductsAdminPageState();
}

class _ProductsAdminPageState extends State<ProductsAdminPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    context.read<AdminProductsCubit>().loadProducts();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _confirmDelete(int id, String name) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Delete Product?'),
        content: Text('Are you sure you want to delete "$name"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<AdminProductsCubit>().deleteProduct(id);
            },
            child: const Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(
      symbol: '\$',
      decimalDigits: 2,
    );

    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (_) => AdminCategoriesCubit(AdminCategoriesRepository()),
        ),
      ],
      child: BlocConsumer<AdminProductsCubit, AdminProductsState>(
        listener: (context, state) {
          if (state is AdminProductsError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.red,
              ),
            );
          }
          if (state is AdminProductActionSuccess) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.green,
              ),
            );
          }
        },
        builder: (context, state) {
          return Column(
            children: [
              // Header
              Padding(
                padding: const EdgeInsets.all(24),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _searchController,
                        decoration: InputDecoration(
                          hintText: 'Search products...',
                          prefixIcon: const Icon(Icons.search),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          suffixIcon: IconButton(
                            icon: const Icon(Icons.clear),
                            onPressed: () {
                              _searchController.clear();
                              context.read<AdminProductsCubit>().search(null);
                            },
                          ),
                        ),
                        onSubmitted: (v) =>
                            context.read<AdminProductsCubit>().search(v.trim()),
                      ),
                    ),
                    const SizedBox(width: 16),
                    ElevatedButton.icon(
                      onPressed: () async {
                        final productsCubit = context
                            .read<AdminProductsCubit>();
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => MultiBlocProvider(
                              providers: [
                                BlocProvider.value(value: productsCubit),
                                BlocProvider(
                                  create: (_) => AdminCategoriesCubit(
                                    AdminCategoriesRepository(),
                                  )..loadCategories(),
                                ),
                              ],
                              child: const ProductFormPage(),
                            ),
                          ),
                        );
                        if (result == true && context.mounted) {
                          productsCubit.loadProducts();
                        }
                      },
                      icon: const Icon(Icons.add),
                      label: const Text('Add Product'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF6C63FF),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 18,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              // Table
              Expanded(child: _buildTable(context, state, currencyFormat)),
            ],
          );
        },
      ),
    );
  }

  Widget _buildTable(
    BuildContext context,
    AdminProductsState state,
    NumberFormat currencyFormat,
  ) {
    if (state is AdminProductsLoading || state is AdminProductsInitial) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state is AdminProductsError) {
      return Center(child: Text(state.message));
    }
    if (state is AdminProductsLoaded) {
      if (state.products.isEmpty) {
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.inventory_2_outlined,
                size: 64,
                color: Colors.grey[400],
              ),
              const SizedBox(height: 12),
              Text('No products yet', style: GoogleFonts.poppins()),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () async {
                  final productsCubit = context.read<AdminProductsCubit>();
                  final result = await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => MultiBlocProvider(
                        providers: [
                          BlocProvider.value(value: productsCubit),
                          BlocProvider(
                            create: (_) => AdminCategoriesCubit(
                              AdminCategoriesRepository(),
                            )..loadCategories(),
                          ),
                        ],
                        child: const ProductFormPage(),
                      ),
                    ),
                  );
                  if (result == true && context.mounted) {
                    productsCubit.loadProducts();
                  }
                },
                icon: const Icon(Icons.add),
                label: const Text('Add First Product'),
              ),
            ],
          ),
        );
      }

      return SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Card(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            children: [
              DataTable(
                headingRowColor: MaterialStateProperty.all(
                  const Color(0xFFF5F5F7),
                ),
                columns: const [
                  DataColumn(label: Text('Image')),
                  DataColumn(label: Text('Name')),
                  DataColumn(label: Text('Category')),
                  DataColumn(label: Text('Price')),
                  DataColumn(label: Text('Stock')),
                  DataColumn(label: Text('Actions')),
                ],
                rows: state.products.map((product) {
                  return DataRow(
                    cells: [
                      DataCell(
                        Container(
                          width: 50,
                          height: 50,
                          decoration: BoxDecoration(
                            color: Colors.grey[100],
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child:
                              product.imageUrl != null &&
                                  product.imageUrl!.isNotEmpty
                              ? ClipRRect(
                                  borderRadius: BorderRadius.circular(8),
                                  child: CachedNetworkImage(
                                    imageUrl: product.imageUrl!,
                                    fit: BoxFit.cover,
                                    errorWidget: (_, __, ___) => Icon(
                                      Icons.broken_image_outlined,
                                      color: Colors.grey[400],
                                    ),
                                  ),
                                )
                              : Icon(
                                  Icons.image_outlined,
                                  color: Colors.grey[400],
                                ),
                        ),
                      ),
                      DataCell(
                        Text(
                          product.name,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                      DataCell(
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF6C63FF).withOpacity(0.15),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            product.categoryName,
                            style: TextStyle(
                              color: const Color(0xFF6C63FF),
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                      DataCell(
                        Text(
                          currencyFormat.format(product.price),
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                      DataCell(
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: product.stockQuantity > 10
                                ? Colors.green.withOpacity(0.15)
                                : product.stockQuantity > 0
                                ? Colors.orange.withOpacity(0.15)
                                : Colors.red.withOpacity(0.15),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            '${product.stockQuantity}',
                            style: TextStyle(
                              color: product.stockQuantity > 10
                                  ? Colors.green
                                  : product.stockQuantity > 0
                                  ? Colors.orange
                                  : Colors.red,
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                      DataCell(
                        Row(
                          children: [
                            IconButton(
                              icon: const Icon(
                                Icons.edit_outlined,
                                size: 20,
                                color: Colors.blue,
                              ),
                              onPressed: () async {
                                final productsCubit = context
                                    .read<AdminProductsCubit>();
                                final result = await Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => MultiBlocProvider(
                                      providers: [
                                        BlocProvider.value(
                                          value: productsCubit,
                                        ),
                                        BlocProvider(
                                          create: (_) => AdminCategoriesCubit(
                                            AdminCategoriesRepository(),
                                          )..loadCategories(),
                                        ),
                                      ],
                                      child: ProductFormPage(product: product),
                                    ),
                                  ),
                                );
                                if (result == true && context.mounted) {
                                  productsCubit.loadProducts();
                                }
                              },
                            ),
                            IconButton(
                              icon: const Icon(
                                Icons.delete_outline,
                                size: 20,
                                color: Colors.red,
                              ),
                              onPressed: () =>
                                  _confirmDelete(product.id, product.name),
                            ),
                          ],
                        ),
                      ),
                    ],
                  );
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
                      '(${state.totalElements} products)',
                      style: GoogleFonts.poppins(fontSize: 13),
                    ),
                    Row(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.chevron_left),
                          onPressed: state.currentPage > 0
                              ? () => context
                                    .read<AdminProductsCubit>()
                                    .loadProducts(page: state.currentPage - 1)
                              : null,
                        ),
                        IconButton(
                          icon: const Icon(Icons.chevron_right),
                          onPressed: state.currentPage < state.totalPages - 1
                              ? () => context
                                    .read<AdminProductsCubit>()
                                    .loadProducts(page: state.currentPage + 1)
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
