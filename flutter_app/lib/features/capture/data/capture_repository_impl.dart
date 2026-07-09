import 'dart:io';

import 'package:bandu_wrong_notebook/core/camera/camera_service.dart';
import 'package:bandu_wrong_notebook/core/storage/capture_file_store.dart';
import 'package:bandu_wrong_notebook/features/capture/data/capture_api_service.dart';
import 'package:bandu_wrong_notebook/features/capture/data/capture_dto_mapper.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

final captureRepositoryProvider = Provider<CaptureRepository>((ref) {
  return RemoteCaptureRepository(
    apiService: ref.watch(captureApiServiceProvider),
    cameraService: ref.watch(captureCameraServiceProvider),
    fileStore: ref.watch(captureFileStoreProvider),
    mapper: const CaptureDtoMapper(),
  );
});

class RemoteCaptureRepository implements CaptureRepository {
  const RemoteCaptureRepository({
    required this.apiService,
    required this.cameraService,
    required this.fileStore,
    required this.mapper,
  });

  final CaptureApiService apiService;
  final CaptureCameraService cameraService;
  final CaptureFileStore fileStore;
  final CaptureDtoMapper mapper;

  @override
  Future<String?> takePhoto() async {
    return _persistPickedImage(await cameraService.takePhoto());
  }

  @override
  Future<String?> pickFromGallery() async {
    return _persistPickedImage(await cameraService.pickFromGallery());
  }

  @override
  Future<AnalyzeResult> analyzeImage(String localImagePath) async {
    final data = await apiService.analyzeImage(localImagePath);
    return mapper.analyzeResultFromJson(data);
  }

  Future<String?> _persistPickedImage(XFile? image) async {
    if (image == null) {
      return null;
    }

    final capturedFile = await fileStore.persistOriginal(File(image.path));
    return capturedFile.path;
  }
}
