import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../cubit/image_upload_cubit.dart';

class ImagePickerWidget extends StatefulWidget {
  final String? initialImageUrl;
  final ValueChanged<String?> onImageChanged;

  const ImagePickerWidget({
    super.key,
    this.initialImageUrl,
    required this.onImageChanged,
  });

  @override
  State<ImagePickerWidget> createState() => _ImagePickerWidgetState();
}

class _ImagePickerWidgetState extends State<ImagePickerWidget> {
  String? _imageUrl;
  final _urlController = TextEditingController();
  bool _isUploading = false;

  @override
  void initState() {
    super.initState();
    _imageUrl = widget.initialImageUrl;
    if (_imageUrl != null && _imageUrl!.isNotEmpty) {
      _urlController.text = _imageUrl!;
    }
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _pickAndUpload() async {
    try {
      final picker = ImagePicker();
      final XFile? file = await picker.pickImage(source: ImageSource.gallery);
      if (file == null) return;

      final bytes = await file.readAsBytes();
      setState(() => _isUploading = true);

      if (!mounted) return;
      context.read<ImageUploadCubit>().uploadImage(
            bytes: bytes,
            filename: file.name,
          );
    } catch (e) {
      if (!mounted) return;
      setState(() => _isUploading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to pick image: $e'), backgroundColor: Colors.red),
      );
    }
  }

  void _applyUrl() {
    final url = _urlController.text.trim();
    setState(() => _imageUrl = url.isEmpty ? null : url);
    widget.onImageChanged(_imageUrl);
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<ImageUploadCubit, ImageUploadState>(
      listener: (context, state) {
        if (state is ImageUploadSuccess) {
          setState(() {
            _imageUrl = state.url;
            _urlController.text = state.url;
            _isUploading = false;
          });
          widget.onImageChanged(state.url);
          context.read<ImageUploadCubit>().reset();
        }
        if (state is ImageUploadError) {
          setState(() => _isUploading = false);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.red),
          );
        }
      },
      builder: (context, state) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Product Image',
                style: GoogleFonts.poppins(fontWeight: FontWeight.w600)),
            const SizedBox(height: 12),

            // Preview
            Container(
              width: double.infinity,
              height: 180,
              decoration: BoxDecoration(
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey[300]!),
              ),
              child: _imageUrl != null && _imageUrl!.isNotEmpty
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: CachedNetworkImage(
                        imageUrl: _imageUrl!,
                        fit: BoxFit.cover,
                        placeholder: (_, __) =>
                            const Center(child: CircularProgressIndicator()),
                        errorWidget: (_, __, ___) => Icon(
                          Icons.broken_image_outlined,
                          size: 48,
                          color: Colors.grey[400],
                        ),
                      ),
                    )
                  : Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.image_outlined,
                            size: 48, color: Colors.grey[400]),
                        const SizedBox(height: 8),
                        Text('No image selected',
                            style: GoogleFonts.poppins(
                                color: Colors.grey[500], fontSize: 13)),
                      ],
                    ),
            ),
            const SizedBox(height: 12),

            // Upload Button
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _isUploading ? null : _pickAndUpload,
                    icon: _isUploading
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.upload_file),
                    label: Text(_isUploading ? 'Uploading...' : 'Upload from Device'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // OR Divider
            Row(
              children: [
                Expanded(child: Divider(color: Colors.grey[300])),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text('OR', style: GoogleFonts.poppins(
                      color: Colors.grey[500], fontSize: 12)),
                ),
                Expanded(child: Divider(color: Colors.grey[300])),
              ],
            ),
            const SizedBox(height: 12),

            // URL Field
            TextField(
              controller: _urlController,
              decoration: InputDecoration(
                labelText: 'Image URL',
                hintText: 'https://example.com/image.jpg',
                prefixIcon: const Icon(Icons.link),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.check),
                  onPressed: _applyUrl,
                ),
              ),
              onSubmitted: (_) => _applyUrl(),
            ),
          ],
        );
      },
    );
  }
}