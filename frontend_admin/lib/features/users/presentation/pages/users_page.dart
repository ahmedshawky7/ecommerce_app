import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../cubit/users_cubit.dart';

class UsersPage extends StatefulWidget {
  const UsersPage({super.key});

  @override
  State<UsersPage> createState() => _UsersPageState();
}

class _UsersPageState extends State<UsersPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    context.read<UsersCubit>().loadUsers();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return BlocConsumer<UsersCubit, UsersState>(
      listener: (context, state) {
        if (state is UsersError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.red),
          );
        }
      },
      builder: (context, state) {
        return Column(
          children: [
            // Filters
            Padding(
              padding: const EdgeInsets.all(24),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _searchController,
                      decoration: InputDecoration(
                        hintText: 'Search by name or email...',
                        prefixIcon: const Icon(Icons.search),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        suffixIcon: IconButton(
                          icon: const Icon(Icons.clear),
                          onPressed: () {
                            _searchController.clear();
                            context.read<UsersCubit>().searchByKeyword(null);
                          },
                        ),
                      ),
                      onSubmitted: (value) => context
                          .read<UsersCubit>()
                          .searchByKeyword(value.trim()),
                    ),
                  ),
                  const SizedBox(width: 16),
                  DropdownButton<String?>(
                    value: state is UsersLoaded ? state.currentRole : null,
                    hint: const Text('Filter by role'),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('All Roles')),
                      DropdownMenuItem(value: 'CUSTOMER', child: Text('Customer')),
                      DropdownMenuItem(value: 'ADMIN', child: Text('Admin')),
                    ],
                    onChanged: (value) =>
                        context.read<UsersCubit>().filterByRole(value),
                  ),
                ],
              ),
            ),
            // Table
            Expanded(child: _buildTable(context, state, dateFormat)),
          ],
        );
      },
    );
  }

  Widget _buildTable(BuildContext context, UsersState state, DateFormat dateFormat) {
    if (state is UsersLoading || state is UsersInitial) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state is UsersError) {
      return Center(child: Text(state.message));
    }
    if (state is UsersLoaded) {
      if (state.users.isEmpty) {
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.people_outline, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 12),
              Text('No users found', style: GoogleFonts.poppins()),
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
                  DataColumn(label: Text('Username')),
                  DataColumn(label: Text('Email')),
                  DataColumn(label: Text('Role')),
                  DataColumn(label: Text('Status')),
                  DataColumn(label: Text('Joined')),
                  DataColumn(label: Text('Actions')),
                ],
                rows: state.users.map((user) {
                  return DataRow(cells: [
                    DataCell(Text(user.id.toString())),
                    DataCell(Text(user.username,
                        style: const TextStyle(fontWeight: FontWeight.w600))),
                    DataCell(Text(user.email)),
                    DataCell(Container(
                      padding:
                          const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: user.role == 'ADMIN'
                            ? const Color(0xFF6C63FF).withOpacity(0.15)
                            : Colors.grey.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        user.role,
                        style: TextStyle(
                          color: user.role == 'ADMIN'
                              ? const Color(0xFF6C63FF)
                              : Colors.grey[700],
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    )),
                    DataCell(Row(
                      children: [
                        Icon(
                          user.isEnabled
                              ? Icons.check_circle
                              : Icons.cancel,
                          color: user.isEnabled ? Colors.green : Colors.red,
                          size: 18,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          user.isEnabled ? 'Active' : 'Disabled',
                          style: TextStyle(
                            color: user.isEnabled ? Colors.green : Colors.red,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    )),
                    DataCell(Text(dateFormat.format(user.createdAt))),
                    DataCell(
                      Switch(
                        value: user.isEnabled,
                        onChanged: (_) {
                          context.read<UsersCubit>().toggleStatus(user.id);
                        },
                      ),
                    ),
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
                      '(${state.totalElements} users)',
                      style: GoogleFonts.poppins(fontSize: 13),
                    ),
                    Row(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.chevron_left),
                          onPressed: state.currentPage > 0
                              ? () => context
                                  .read<UsersCubit>()
                                  .loadUsers(page: state.currentPage - 1)
                              : null,
                        ),
                        IconButton(
                          icon: const Icon(Icons.chevron_right),
                          onPressed: state.currentPage < state.totalPages - 1
                              ? () => context
                                  .read<UsersCubit>()
                                  .loadUsers(page: state.currentPage + 1)
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