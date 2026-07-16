import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_repository.dart';

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

class SaveAnalyzedCaptureUseCase {
  const SaveAnalyzedCaptureUseCase(this._repository);

  final CaptureRepository _repository;

  Future<SavedErrorItem> call({
    required String localImagePath,
    required AnalyzeResult result,
    required String requestId,
  }) {
    return _repository.saveAnalysis(
      localImagePath: localImagePath,
      result: result,
      requestId: requestId,
    );
  }
}

class CancelCaptureRequestUseCase {
  const CancelCaptureRequestUseCase(this._repository);

  final CaptureRepository _repository;

  void call() {
    _repository.cancelOngoingRequest();
  }
}
