import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

final captureApiServiceProvider = Provider<CaptureApiService>((ref) {
  return CaptureApiService(ref.watch(apiClientProvider));
});

class CaptureApiService {
  const CaptureApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<Map<String, Object?>> analyzeImage(String localImagePath) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(
        localImagePath,
        filename: p.basename(localImagePath),
      ),
    });

    return _apiClient.post<Map<String, Object?>>(
      '/analyze',
      data: formData,
    );
  }
}
