import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/capture/capture_providers.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/pending_task_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final captureControllerProvider =
    NotifierProvider<CaptureController, CaptureUiState>(CaptureController.new);

class CaptureController extends Notifier<CaptureUiState> {
  @override
  CaptureUiState build() => CaptureUiState.initial();

  Future<bool> takePhoto() async {
    return _pickImage(ref.read(takePhotoUseCaseProvider).call);
  }

  Future<bool> pickFromGallery() async {
    return _pickImage(ref.read(pickCaptureImageUseCaseProvider).call);
  }

  Future<void> analyze() async {
    final path = state.localImagePath;
    if (path == null) {
      return;
    }

    state = CaptureUiState(phase: CapturePhase.analyzing, localImagePath: path);
    try {
      final result = await ref.read(analyzeCaptureUseCaseProvider).call(path);
      state = CaptureUiState(
        phase: CapturePhase.success,
        localImagePath: path,
        result: result,
      );
    } catch (error) {
      if (error is AppFailure && error.isCancellation) {
        if (state.phase == CapturePhase.analyzing) {
          state = CaptureUiState(
            phase: CapturePhase.preview,
            localImagePath: path,
          );
        }
        return;
      }
      state = CaptureUiState(
        phase: CapturePhase.failed,
        localImagePath: path,
        errorMessage: appFailureUserMessage(error),
      );
    }
  }

  Future<void> saveToLibrary() async {
    final path = state.localImagePath;
    final result = state.result;
    if (path == null || result == null || state.savedErrorItemId != null) {
      return;
    }

    state = CaptureUiState(
      phase: CapturePhase.uploading,
      localImagePath: path,
      result: result,
    );
    try {
      final task = await ref.read(queueAnalyzedCaptureUseCaseProvider).call(
            localImagePath: path,
            result: result,
          );
      state = CaptureUiState(
        phase: CapturePhase.uploading,
        localImagePath: path,
        result: result,
        queuedTaskId: task.id,
      );
      final upload =
          await ref.read(retryPendingTaskUseCaseProvider).call(task.id);
      if (upload.isSuccess) {
        ref.invalidate(libraryControllerProvider);
        ref.invalidate(statsOverviewProvider);
        state = CaptureUiState(
          phase: CapturePhase.success,
          result: result,
          savedErrorItemId: upload.savedErrorItemId,
          noticeMessage: upload.message,
        );
      } else {
        state = CaptureUiState(
          phase: CapturePhase.success,
          localImagePath: path,
          result: result,
          queuedTaskId: task.id,
          noticeMessage: '${upload.message} 任务已保存在本机。',
        );
      }
    } catch (error) {
      if (error is AppFailure && error.isCancellation) {
        if (state.phase == CapturePhase.uploading) {
          state = CaptureUiState(
            phase: CapturePhase.success,
            localImagePath: path,
            result: result,
            queuedTaskId: state.queuedTaskId,
          );
        }
        return;
      }
      state = CaptureUiState(
        phase: CapturePhase.failed,
        localImagePath: path,
        result: result,
        queuedTaskId: state.queuedTaskId,
        errorMessage: appFailureUserMessage(error),
      );
    }
  }

  void reset() {
    ref.read(cancelCaptureRequestUseCaseProvider).call();
    state = CaptureUiState.initial();
  }

  void cancelOngoingRequest() {
    final current = state;
    final queuedTaskId = current.queuedTaskId;
    if (current.phase == CapturePhase.uploading && queuedTaskId != null) {
      ref.read(cancelPendingTaskUseCaseProvider).call(queuedTaskId);
    } else {
      ref.read(cancelCaptureRequestUseCaseProvider).call();
    }
    if (current.phase == CapturePhase.analyzing) {
      state = CaptureUiState(
        phase: CapturePhase.preview,
        localImagePath: current.localImagePath,
      );
    } else if (current.phase == CapturePhase.uploading) {
      state = CaptureUiState(
        phase: CapturePhase.success,
        localImagePath: current.localImagePath,
        result: current.result,
        queuedTaskId: queuedTaskId,
        noticeMessage: queuedTaskId == null ? null : '上传已取消，任务仍保留在本机。',
      );
    }
  }

  Future<bool> _pickImage(Future<String?> Function() picker) async {
    state = const CaptureUiState(phase: CapturePhase.capturing);
    try {
      final path = await picker();
      if (path == null) {
        state = CaptureUiState.initial();
        return false;
      }
      state = CaptureUiState(phase: CapturePhase.preview, localImagePath: path);
      return true;
    } catch (error) {
      state = CaptureUiState(
        phase: CapturePhase.failed,
        errorMessage: appFailureUserMessage(error),
      );
      return false;
    }
  }
}
