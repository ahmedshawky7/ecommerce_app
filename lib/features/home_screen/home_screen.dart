import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/styling/theme_cubit.dart';
import 'package:ecommerce_app/core/widgets/custom_text_field.dart';
import 'package:ecommerce_app/core/widgets/primary_button_widget.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/home_screen/cubit/categories_cubit.dart';
import 'package:ecommerce_app/features/home_screen/cubit/categories_state.dart';
import 'package:ecommerce_app/features/home_screen/cubit/product_cubit.dart';
import 'package:ecommerce_app/features/home_screen/cubit/product_state.dart';
import 'package:ecommerce_app/features/home_screen/widgets/category_item_widget.dart';
import 'package:ecommerce_app/features/home_screen/widgets/product_item_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_staggered_animations/flutter_staggered_animations.dart';
import 'package:go_router/go_router.dart';
import 'package:shimmer/shimmer.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String selectedCategory = 'All';

  @override
  Widget build(BuildContext context) {
    // final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text('Discover', style: AppStyles.primaryHeadLineStyle(context)),
        actions: [
          BlocBuilder<ThemeCubit, ThemeMode>(
            builder: (context, themeMode) {
              return IconButton(
                onPressed: () {
                  context.read<ThemeCubit>().toggleTheme(context);
                },
                icon: Icon(
                  themeMode == ThemeMode.light
                      ? Icons.dark_mode
                      : Icons.light_mode,
                ),
              );
            },
          ),
        ],
      ),
      body: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.w),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            HeightSpace(16),

            // البحث
            // البحث
            Row(
              children: [
                Expanded(
                  child: CustomTextField(
                    hintText: 'Search for clothes...',
                    prefixIcon: const Icon(Icons.search),
                  ),
                ),
                WidthSpace(12),
                Container(
                  width: 52.w,
                  height: 52.h,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary,
                    borderRadius: BorderRadius.circular(10.r),
                  ),
                  child: const Icon(
                    Icons.send_and_archive_outlined,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
            HeightSpace(16),

            // الفئات
            BlocBuilder<CategoriesCubit, CategoriesState>(
              builder: (context, state) {
                if (state is CategoriesLoading) {
                  return SizedBox(
                    height: 60.h,
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                if (state is CategoriesLoaded) {
                  return SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: state.categories.map((cat) {
                        return CategoryItemWidget(
                          title: cat.name,
                          isSelected: selectedCategory == cat.slug,
                          onTap: () {
                            if (selectedCategory != cat.slug) {
                              setState(() => selectedCategory = cat.slug);
                              if (selectedCategory == 'All') {
                                context.read<ProductCubit>().getAllProducts();
                              } else {
                                context
                                    .read<ProductCubit>()
                                    .getProductsByCategory(cat.slug);
                              }
                            }
                          },
                        );
                      }).toList(),
                    ),
                  );
                }
                if (state is CategoriesError) {
                  return Text('Failed to load categories');
                }
                return SizedBox.shrink();
              },
            ),
            HeightSpace(16),

            // المنتجات
            Expanded(
              child: BlocBuilder<ProductCubit, ProductState>(
                buildWhen: (previous, current) => current is! ProductLoading,
                builder: (context, state) {
                  if (state is ProductLoading) {
                    return Shimmer.fromColors(
                      baseColor: Theme.of(
                        context,
                      ).colorScheme.onSurface.withOpacity(0.1),
                      highlightColor: Theme.of(context).colorScheme.primary,
                      child: GridView.builder(
                        itemCount: 6,
                        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: 16.h,
                          crossAxisSpacing: 16.w,
                          childAspectRatio: 0.75,
                        ),
                        itemBuilder: (context, index) {
                          return Container(
                            decoration: BoxDecoration(
                              color: Theme.of(
                                context,
                              ).colorScheme.onSurface.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(8.r),
                            ),
                          );
                        },
                      ),
                    );
                  }

                  if (state is ProductLoaded) {
                    return RefreshIndicator(
                      color: Theme.of(context).colorScheme.primary,
                      backgroundColor: Theme.of(
                        context,
                      ).scaffoldBackgroundColor,
                      onRefresh: () async {
                        setState(() => selectedCategory = 'All');
                        context.read<ProductCubit>().getAllProducts();
                      },
                      child: AnimationLimiter(
                        child: GridView.builder(
                          key: const PageStorageKey('productsGrid'),
                          itemCount: state.products.length,
                          gridDelegate:
                              SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                mainAxisSpacing: 16.h,
                                crossAxisSpacing: 16.w,
                                childAspectRatio: 0.75,
                              ),
                          itemBuilder: (context, index) {
                            final product = state.products[index];
                            return AnimationConfiguration.staggeredGrid(
                              position: index,
                              columnCount: 2,
                              duration: const Duration(milliseconds: 600),
                              child: SlideAnimation(
                                verticalOffset: 200.0,
                                child: FadeInAnimation(
                                  child: ProductItemWidget(
                                    title: product.title,
                                    price: product.price.toStringAsFixed(2),
                                    imageUrl: product.thumbnail,
                                    onTap: () {
                                      context.pushNamed(
                                        AppRoutes.productScreen,
                                        extra: product,
                                      );
                                    },
                                  ),
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                    );
                  }

                  if (state is ProductError) {
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text('Error: ${state.message}'),
                          HeightSpace(16),
                          PrimaryButtonWidget(
                            buttonText: 'Retry',
                            onPressed: () =>
                                context.read<ProductCubit>().getAllProducts(),
                          ),
                        ],
                      ),
                    );
                  }

                  return SizedBox.shrink();
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
