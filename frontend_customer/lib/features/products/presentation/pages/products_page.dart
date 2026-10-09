import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/widgets/product_card.dart';
import '../cubit/categories_cubit.dart';
import '../cubit/products_cubit.dart';

class ProductsPage extends StatefulWidget {
  const ProductsPage({super.key});

  @override
  State<ProductsPage> createState() => _ProductsPageState();
}

class _ProductsPageState extends State<ProductsPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    context.read<ProductsCubit>().loadProducts();
    context.read<CategoriesCubit>().loadCategories();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Search Bar
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search products...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                icon: const Icon(Icons.clear),
                onPressed: () {
                  _searchController.clear();
                  context.read<ProductsCubit>().clearFilters();
                },
              ),
            ),
            onSubmitted: (value) =>
                context.read<ProductsCubit>().search(value.trim()),
          ),
        ),
        // Categories Chips
        BlocBuilder<CategoriesCubit, CategoriesState>(
          builder: (context, state) {
            if (state is CategoriesLoaded) {
              return SizedBox(
                height: 50,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  itemCount: state.categories.length + 1,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final currentProductsState = context.read<ProductsCubit>().state;
                    final selectedId = currentProductsState is ProductsLoaded
                        ? currentProductsState.selectedCategoryId
                        : null;

                    if (index == 0) {
                      final isAll = selectedId == null;
                      return ChoiceChip(
                        label: const Text('All'),
                        selected: isAll,
                        onSelected: (_) =>
                            context.read<ProductsCubit>().filterByCategory(null),
                        selectedColor:
                            const Color(0xFF6C63FF).withOpacity(0.2),
                        labelStyle: GoogleFonts.poppins(
                          color: isAll ? const Color(0xFF6C63FF) : null,
                          fontWeight:
                              isAll ? FontWeight.w600 : FontWeight.normal,
                        ),
                      );
                    }

                    final cat = state.categories[index - 1];
                    final isSelected = selectedId == cat.id;
                    return ChoiceChip(
                      label: Text(cat.name),
                      selected: isSelected,
                      onSelected: (_) => context
                          .read<ProductsCubit>()
                          .filterByCategory(cat.id),
                      selectedColor:
                          const Color(0xFF6C63FF).withOpacity(0.2),
                      labelStyle: GoogleFonts.poppins(
                        color: isSelected ? const Color(0xFF6C63FF) : null,
                        fontWeight:
                            isSelected ? FontWeight.w600 : FontWeight.normal,
                      ),
                    );
                  },
                ),
              );
            }
            return const SizedBox(height: 50);
          },
        ),
        const SizedBox(height: 8),
        // Products Grid
        Expanded(
          child: BlocBuilder<ProductsCubit, ProductsState>(
            builder: (context, state) {
              if (state is ProductsLoading || state is ProductsInitial) {
                return const Center(child: CircularProgressIndicator());
              }
              if (state is ProductsError) {
                return Center(child: Text(state.message));
              }
              if (state is ProductsLoaded) {
                if (state.products.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.search_off, size: 64, color: Colors.grey[400]),
                        const SizedBox(height: 12),
                        Text('No products found',
                            style: GoogleFonts.poppins()),
                      ],
                    ),
                  );
                }
                return GridView.builder(
                  padding: const EdgeInsets.all(20),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 240,
                    childAspectRatio: 0.7,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                  ),
                  itemCount: state.products.length,
                  itemBuilder: (context, index) {
                    return ProductCard(product: state.products[index]);
                  },
                );
              }
              return const SizedBox.shrink();
            },
          ),
        ),
      ],
    );
  }
}