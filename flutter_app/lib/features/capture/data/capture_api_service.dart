import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
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

  Future<Map<String, Object?>> saveAnalysis({
    required String localImagePath,
    required AnalyzeResult result,
  }) async {
    final file = File(localImagePath);
    final imageBase64 = base64Encode(await file.readAsBytes());
    final extension = p.extension(localImagePath).toLowerCase();
    final mimeType = switch (extension) {
      '.png' => 'image/png',
      '.webp' => 'image/webp',
      _ => 'image/jpeg',
    };

    return _apiClient.post<Map<String, Object?>>(
      '/error-items',
      data: {
        'questionText': result.questionText ?? result.title,
        'answer': result.answer,
        'analysis': result.analysis,
        'subjectName': result.subjectName,
        'tags': result.tags,
        'originalImageUrl': 'data:$mimeType;base64,$imageBase64',
      },
    );
  }
}
