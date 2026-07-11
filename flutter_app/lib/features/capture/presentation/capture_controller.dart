import 'package:bandu_wrong_notebook/features/capture/capture_providers.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/features/stats/data/stats_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final captureControllerProvider =
    NotifierProvider<CaptureController, CaptureUiState>(CaptureController.new);

class CaptureController extends Notifier<CaptureUiState> {
  @override
  CaptureUiState build() => CaptureUiState.initial();

  Future<void> takePhoto() async {
    await _pickImage(ref.read(takePhotoUseCaseProvider).call);
  }

  Future<void> pickFromGallery() async {
    await _pickImage(ref.read(pickCaptureImageUseCaseProvider).call);
  }

  Future<void> analyze() async {
    final path = state.localImagePath;
    if (path == null) {
      return;
    }

    state = state.copyWith(phase: CapturePhase.analyzing, errorMessage: null);
    try {
      final result = await ref.read(analyzeCaptureUseCaseProvider).call(path);
      state = state.copyWith(
        phase: CapturePhase.success,
        result: result,
        savedErrorItemId: null,
        errorMessage: null,
      );
    } catch (error) {
      state = state.copyWith(
        phase: CapturePhase.success,
        errorMessage: error.toString(),
      );
    }
  }

  Future<void> saveToLibrary() async {
    final path = state.localImagePath;
    final result = state.result;
    if (path == null || result == null || state.savedErrorItemId != null) {
      return;
    }

    state = state.copyWith(phase: CapturePhase.uploading, errorMessage: null);
    try {
      final saved = await ref.read(saveAnalyzedCaptureUseCaseProvider).call(
            localImagePath: path,
            result: result,
      );
      ref.invalidate(libraryControllerProvider);
      ref.invalidate(statsOverviewProvider);
      state = state.copyWith(
        phase: CapturePhase.success,
        savedErrorItemId: saved.id,
        errorMessage: null,
      );
    } catch (error) {
      state = state.copyWith(
        phase: CapturePhase.failed,
        errorMessage: error.toString(),
      );
    }
  }

  void reset() {
    state = CaptureUiState.initial();
  }

  Future<void> _pickImage(Future<String?> Function() picker) async {
    state = state.copyWith(phase: CapturePhase.capturing, errorMessage: null);
    try {
      final path = await picker();
      if (path == null) {
        state = CaptureUiState.initial();
        return;
      }
      state = CaptureUiState(
        phase: CapturePhase.preview,
        localImagePath: path,
      );
    } catch (error) {
      state = state.copyWith(
        phase: CapturePhase.failed,
        errorMessage: error.toString(),
      );
    }
  }
}
