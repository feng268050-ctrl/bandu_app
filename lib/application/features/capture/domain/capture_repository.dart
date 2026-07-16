import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';

abstract interface class CaptureRepository {
  Future<String?> takePhoto();

  Future<String?> pickFromGallery();

  Future<AnalyzeResult> analyzeImage(String localImagePath);

  Future<SavedErrorItem> saveAnalysis({
    required String localImagePath,
    required AnalyzeResult result,
    required String requestId,
  });

  void cancelOngoingRequest();
}
