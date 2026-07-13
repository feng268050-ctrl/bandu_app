import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/conversion/api/capture/capture_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
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

  Future<AnalyzeResultDto> analyzeImage(String localImagePath) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(
        localImagePath,
        filename: p.basename(localImagePath),
      ),
    });
    final payload = await _apiClient.post<Object?>(
      '/analyze',
      data: formData,
    );
    return AnalyzeResultDto.fromJson(
      requireJsonObject(payload, context: 'analyze response'),
    );
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

  Future<SavedErrorItemDto> saveAnalysis(
    SaveAnalyzedCaptureRequestDto request,
  ) async {
    final payload = await _apiClient.post<Object?>(
      '/error-items',
      data: request.toJson(),
    );
    return SavedErrorItemDto.fromJson(
      requireJsonObject(payload, context: 'save analysis response'),
    );
  }
}
