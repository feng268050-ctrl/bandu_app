import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_repository.dart';

class TakePhotoUseCase {
  const TakePhotoUseCase(this._repository);

  final CaptureRepository _repository;

  Future<String?> call() {
    return _repository.takePhoto();
  }
}

class PickCaptureImageUseCase {
  const PickCaptureImageUseCase(this._repository);

  final CaptureRepository _repository;

  Future<String?> call() {
    return _repository.pickFromGallery();
  }
}

class AnalyzeCaptureUseCase {
  const AnalyzeCaptureUseCase(this._repository);

  final CaptureRepository _repository;

  Future<AnalyzeResult> call(String localImagePath) {
    return _repository.analyzeImage(localImagePath);
  }
}
