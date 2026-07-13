import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
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

    return _apiClient.post<Map<String, Object?>>('/analyze', data: formData);
  }

  Future<String> encodeImageDataUrl(String localImagePath) async {
    final file = File(localImagePath);
    final imageBase64 = base64Encode(await file.readAsBytes());
    final extension = p.extension(localImagePath).toLowerCase();
    final mimeType = switch (extension) {
      '.png' => 'image/png',
      '.webp' => 'image/webp',
      _ => 'image/jpeg',
    };

    return 'data:$mimeType;base64,$imageBase64';
  }

  Future<Map<String, Object?>> saveAnalysis({
    required Map<String, Object?> request,
  }) {
    return _apiClient.post<Map<String, Object?>>(
      '/error-items',
      data: request,
    );
  }
}
