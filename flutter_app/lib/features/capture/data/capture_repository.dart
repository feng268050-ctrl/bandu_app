import 'dart:io';

import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/core/storage/capture_file_store.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as p;

final captureRepositoryProvider = Provider<CaptureRepository>((ref) {
  return RemoteCaptureRepository(
    apiClient: ref.watch(apiClientProvider),
    fileStore: ref.watch(captureFileStoreProvider),
  );
});

abstract interface class CaptureRepository {
  Future<String?> takePhoto();

  Future<String?> pickFromGallery();

  Future<AnalyzeResult> analyzeImage(String localImagePath);
}

class RemoteCaptureRepository implements CaptureRepository {
  RemoteCaptureRepository({
    required this.apiClient,
    required this.fileStore,
    ImagePicker? picker,
  }) : _picker = picker ?? ImagePicker();

  final ApiClient apiClient;
  final CaptureFileStore fileStore;
  final ImagePicker _picker;

  @override
  Future<String?> takePhoto() async {
    final image = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 92,
    );
    return _persistPickedImage(image);
  }

  @override
  Future<String?> pickFromGallery() async {
    final image = await _picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 92,
    );
    return _persistPickedImage(image);
  }

  @override
  Future<AnalyzeResult> analyzeImage(String localImagePath) async {
    final formData = FormData.fromMap({
      'image': await MultipartFile.fromFile(
        localImagePath,
        filename: p.basename(localImagePath),
      ),
    });

    final data = await apiClient.post<Map<String, Object?>>(
      '/analyze',
      data: formData,
    );
    return AnalyzeResult.fromJson(data);
  }

  Future<String?> _persistPickedImage(XFile? image) async {
    if (image == null) {
      return null;
    }

    final capturedFile = await fileStore.persistOriginal(File(image.path));
    return capturedFile.path;
  }
}
