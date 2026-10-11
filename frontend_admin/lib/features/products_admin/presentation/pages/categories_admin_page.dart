import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import '../cubit/admin_categories_cubit.dart';
import '../cubit/image_upload_cubit.dart';
import '../../data/repositories/image_upload_repository.dart';

class CategoriesAdminPage extends StatefulWidget {
  const CategoriesAdminPage({super.key});

  @override
  State<CategoriesAdminPage> createState() => _CategoriesAdminPageState();
}

class _CategoriesAdminPageState extends State<CategoriesAdminPage> {
  @override
  void initState() {
    super.initState();
    context.read<AdminCategoriesCubit>().loadCategories();
  }

  void _showCategoryDialog({dynamic existing}) {
    final nameController = TextEditingController(text: existing?.name ?? '');
    final descController =
        TextEditingController(text: existing?.description ?? '');
    String? imageUrl = existing?.imageUrl;

    showDialog(
      context: context,
      builder: (dialogContext) {
        return BlocProvider(
          create: (_) => ImageUploadCubit(ImageUploadRepository()),
          child: StatefulBuilder(
            builder: (context, setDialogState) {
              return AlertDialog(
                title: Text(existing == null ? 'Add Category' : 'Edit Category'),
                content: SingleChildScrollView(
                  child: SizedBox(
                    width: 400,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextField(
                          controller: nameController,
                          decoration: const InputDecoration(
                            labelText: 'Category Name *',
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: descController,
                          maxLines: 3,
                          decoration: const InputDecoration(
                            labelText: 'Description',
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: TextEditingController(text: imageUrl),
                          decoration: const InputDecoration(
                            labelText: 'Image URL (optional)',
                            border: OutlineInputBorder(),
                          ),
                          onChanged: (v) => imageUrl = v.trim(),
                        ),
                      ],
                    ),
                  ),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(dialogContext),
                    child: const Text('Cancel'),
                  ),
                  ElevatedButton(
                    onPressed: () async {
                      if (nameController.text.trim().isEmpty) return;

                      final cubit = context.read<AdminCategoriesCubit>();
                      if (existing == null) {
                        await cubit.createCategory(
                          name: nameController.text.trim(),
                          description: descController.text.trim(),
                          imageUrl: imageUrl,
                        );
                      } else {
                        await cubit.updateCategory(
                          id: existing.id,
                          name: nameController.text.trim(),
                          description: descController.text.trim(),
                          imageUrl: imageUrl,
                        );
                      }
                      if (context.mounted) Navigator.pop(dialogContext);
                    },
                    child: Text(existing == null ? 'Create' : 'Update'),
                  ),
                ],
              );
            },
          ),
        );
      },
    );
  }

  void _confirmDelete(int id, String name) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Delete Category?'),
        content: Text('Are you sure you want to delete "$name"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<AdminCategoriesCubit>().deleteCategory(id);
            },
            child: const Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<AdminCategoriesCubit, AdminCategoriesState>(
      listener: (context, state) {
        if (state is AdminCategoriesError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.red),
          );
        }
        if (state is AdminCategoryActionSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.green),
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
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Categories Management',
                    style: GoogleFonts.poppins(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  ElevatedButton.icon(
                    onPressed: () => _showCategoryDialog(),
                    icon: const Icon(Icons.add),
                    label: const Text('Add Category'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF6C63FF),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 18),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            // List
            Expanded(child: _buildList(context, state)),
          ],
        );
      },
    );
  }

  Widget _buildList(BuildContext context, AdminCategoriesState state) {
    if (state is AdminCategoriesLoading || state is AdminCategoriesInitial) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state is AdminCategoriesError) {
      return Center(child: Text(state.message));
    }
    if (state is AdminCategoriesLoaded) {
      if (state.categories.isEmpty) {
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.category_outlined, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 12),
              Text('No categories yet', style: GoogleFonts.poppins()),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () => _showCategoryDialog(),
                icon: const Icon(Icons.add),
                label: const Text('Add First Category'),
              ),
            ],
          ),
        );
      }

      return GridView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
          maxCrossAxisExtent: 400,
          childAspectRatio: 2.5,
          crossAxisSpacing: 16,
          mainAxisSpacing: 16,
        ),
        itemCount: state.categories.length,
        itemBuilder: (context, index) {
          final category = state.categories[index];
          return Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  // Image
                  Container(
                    width: 70,
                    height: 70,
                    decoration: BoxDecoration(
                      color: const Color(0xFF6C63FF).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: category.imageUrl != null &&
                            category.imageUrl!.isNotEmpty
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(12),
                            child: CachedNetworkImage(
                              imageUrl: category.imageUrl!,
                              fit: BoxFit.cover,
                              errorWidget: (_, __, ___) => const Icon(
                                Icons.category,
                                color: Color(0xFF6C63FF),
                                size: 32,
                              ),
                            ),
                          )
                        : const Icon(
                            Icons.category,
                            color: Color(0xFF6C63FF),
                            size: 32,
                          ),
                  ),
                  const SizedBox(width: 12),
                  // Info
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          category.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: GoogleFonts.poppins(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          category.description ?? 'No description',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: GoogleFonts.poppins(
                            fontSize: 12,
                            color: Colors.grey[600],
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${category.productCount} products',
                          style: GoogleFonts.poppins(
                            fontSize: 11,
                            color: const Color(0xFF6C63FF),
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  // Actions
                  Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.edit_outlined,
                            size: 20, color: Colors.blue),
                        onPressed: () =>
                            _showCategoryDialog(existing: category),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete_outline,
                            size: 20, color: Colors.red),
                        onPressed: () =>
                            _confirmDelete(category.id, category.name),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      );
    }
    return const SizedBox.shrink();
  }
}