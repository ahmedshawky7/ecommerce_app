import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/repositories/image_upload_repository.dart';

part 'image_upload_state.dart';

class ImageUploadCubit extends Cubit<ImageUploadState> {
  final ImageUploadRepository _repository;
  ImageUploadCubit(this._repository) : super(ImageUploadInitial());

  Future<void> uploadImage({
    required List<int> bytes,
    required String filename,
  }) async {
    emit(ImageUploadLoading());
    try {
      final url = await _repository.uploadImage(
        bytes: bytes,
        filename: filename,
      );
      emit(ImageUploadSuccess(url));
    } catch (e) {
      emit(ImageUploadError(e.toString()));
    }
  }

  void reset() {
    emit(ImageUploadInitial());
  }
}