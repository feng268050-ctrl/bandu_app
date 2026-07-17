import 'dart:io';

import 'package:bandu_wrong_notebook/framework/camera/camera_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:bandu_wrong_notebook/framework/persistence/files/capture_file_store.dart';
import 'package:bandu_wrong_notebook/framework/network/services/capture_api_service.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:bandu_wrong_notebook/conversion/api/capture/capture_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_repository.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/application/ai_request_model_selection.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

final remoteCaptureRepositoryProvider = Provider<CaptureRepository>((ref) {
  return RemoteCaptureRepository(
    apiService: ref.watch(captureApiServiceProvider),
    cameraService: ref.watch(captureCameraServiceProvider),
    fileStore: ref.watch(captureFileStoreProvider),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    mapper: const CaptureDtoMapper(),
    cacheMapper: const ErrorItemCacheMapper(),
    modelSelection: AiRequestModelSelection(
      ref.watch(aiPreferenceRepositoryProvider),
    ),
  );
});

final backgroundCaptureRepositoryProvider = Provider<CaptureRepository>((ref) {
  return RemoteCaptureRepository(
    apiService: CaptureApiService(ref.watch(apiClientProvider)),
    cameraService: ref.watch(captureCameraServiceProvider),
    fileStore: ref.watch(captureFileStoreProvider),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    mapper: const CaptureDtoMapper(),
    cacheMapper: const ErrorItemCacheMapper(),
    modelSelection: AiRequestModelSelection(
      ref.watch(aiPreferenceRepositoryProvider),
    ),
  );
});

class RemoteCaptureRepository implements CaptureRepository {
  const RemoteCaptureRepository({
    required this.apiService,
    required this.cameraService,
    required this.fileStore,
    required this.cacheDatabase,
    required this.mapper,
    required this.cacheMapper,
    required this.modelSelection,
  });

  final CaptureApiService apiService;
  final CaptureCameraService cameraService;
  final CaptureFileStore fileStore;
  final AppCacheDatabase cacheDatabase;
  final CaptureDtoMapper mapper;
  final ErrorItemCacheMapper cacheMapper;
  final AiRequestModelSelection modelSelection;

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
    final dto = await apiService.analyzeImage(
      localImagePath,
      preference: await modelSelection.forPurpose(AiPurpose.visionAnalyze),
    );
    return mapper.analyzeResultFromDto(dto);
  }

  @override
  Future<SavedErrorItem> saveAnalysis({
    required String localImagePath,
    required AnalyzeResult result,
    required String requestId,
  }) async {
    final originalImageUrl =
        await apiService.encodeImageDataUrl(localImagePath);
    final dto = await apiService.saveAnalysis(
      mapper.saveRequest(
        result,
        originalImageUrl: originalImageUrl,
      ),
      requestId: requestId,
    );
    final saved = mapper.savedErrorItemFromDto(dto);
    await cacheDatabase.upsertErrorItemDetail(
      cacheMapper.savedItemToCache(saved),
    );
    return saved;
  }

  @override
  void cancelOngoingRequest() {
    apiService.cancelActiveRequest();
  }

  Future<String?> _persistPickedImage(XFile? image) async {
    if (image == null) {
      return null;
    }

    final capturedFile = await fileStore.persistOriginal(File(image.path));
    return capturedFile.path;
  }
}
