import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

final captureCameraServiceProvider = Provider<CaptureCameraService>((ref) {
  return ImagePickerCaptureCameraService(ImagePicker());
});

abstract interface class CaptureCameraService {
  Future<XFile?> takePhoto();

  Future<XFile?> pickFromGallery();
}

class ImagePickerCaptureCameraService implements CaptureCameraService {
  const ImagePickerCaptureCameraService(this._picker);

  final ImagePicker _picker;

  @override
  Future<XFile?> takePhoto() {
    return _picker.pickImage(source: ImageSource.camera, imageQuality: 92);
  }

  @override
  Future<XFile?> pickFromGallery() {
    return _picker.pickImage(source: ImageSource.gallery, imageQuality: 92);
  }
}
