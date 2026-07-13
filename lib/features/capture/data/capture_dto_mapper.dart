import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';

class CaptureDtoMapper {
  const CaptureDtoMapper();

  AnalyzeResult analyzeResultFromJson(Map<String, Object?> data) {
    return AnalyzeResult.fromJson(data);
  }

  SavedErrorItem savedErrorItemFromJson(Map<String, Object?> data) {
    return SavedErrorItem.fromJson(data);
  }
}
