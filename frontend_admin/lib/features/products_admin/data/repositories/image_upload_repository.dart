import 'package:dio/dio.dart';
import '../../../../core/network/dio_client.dart';

class ImageUploadRepository {
  final _dio = DioClient.instance.client;

  /// Uploads image bytes to Cloudinary via backend.
  /// Returns the secure URL.
  Future<String> uploadImage({
    required List<int> bytes,
    required String filename,
  }) async {
    try {
      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      });

      final response = await _dio.post(
        '/api/images/upload',
        data: formData,
        options: Options(contentType: 'multipart/form-data'),
      );

      return response.data['url'] as String;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data['message'] != null) {
      return e.response!.data['message'];
    }
    if (e.type == DioExceptionType.connectionTimeout) {
      return 'Connection timeout';
    }
    return 'Failed to upload image';
  }
}