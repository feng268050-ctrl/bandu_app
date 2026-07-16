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
  CaptureApiService(this._apiClient);

  final ApiClient _apiClient;
  CancelToken? _activeRequest;

  Future<AnalyzeResultDto> analyzeImage(String localImagePath) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(
        localImagePath,
        filename: p.basename(localImagePath),
      ),
    });
    final cancelToken = CancelToken();
    _activeRequest = cancelToken;
    try {
      final payload = await _apiClient.post<Object?>(
        'analyze',
        data: formData,
        timeouts: aiRequestTimeouts,
        cancelToken: cancelToken,
      );
      return AnalyzeResultDto.fromJson(
        requireJsonObject(payload, context: 'analyze response'),
      );
    } finally {
      if (identical(_activeRequest, cancelToken)) {
        _activeRequest = null;
      }
    }
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
    SaveAnalyzedCaptureRequestDto request, {
    required String requestId,
  }) async {
    final cancelToken = CancelToken();
    _activeRequest = cancelToken;
    try {
      final payload = await _apiClient.post<Object?>(
        'error-items',
        data: request.toJson(),
        headers: {'Idempotency-Key': requestId},
        timeouts: uploadRequestTimeouts,
        cancelToken: cancelToken,
      );
      return SavedErrorItemDto.fromJson(
        requireJsonObject(payload, context: 'save analysis response'),
      );
    } finally {
      if (identical(_activeRequest, cancelToken)) {
        _activeRequest = null;
      }
    }
  }

  void cancelActiveRequest() {
    final request = _activeRequest;
    if (request != null && !request.isCancelled) {
      request.cancel('Cancelled by user');
    }
  }
}
