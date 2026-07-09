import 'package:bandu_wrong_notebook/features/capture/data/capture_repository.dart';
import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final captureControllerProvider =
    NotifierProvider<CaptureController, CaptureUiState>(CaptureController.new);

class CaptureController extends Notifier<CaptureUiState> {
  CaptureRepository get _repository => ref.read(captureRepositoryProvider);

  @override
  CaptureUiState build() => CaptureUiState.initial();

  Future<void> takePhoto() async {
    await _pickImage(_repository.takePhoto);
  }

  Future<void> pickFromGallery() async {
    await _pickImage(_repository.pickFromGallery);
  }

  Future<void> analyze() async {
    final path = state.localImagePath;
    if (path == null) {
      return;
    }

    state = state.copyWith(phase: CapturePhase.analyzing, errorMessage: null);
    try {
      final result = await _repository.analyzeImage(path);
      state = state.copyWith(
        phase: CapturePhase.success,
        result: result,
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
