import 'package:bandu_wrong_notebook/features/capture/application/capture_use_cases.dart';
import 'package:bandu_wrong_notebook/features/capture/data/capture_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final takePhotoUseCaseProvider = Provider<TakePhotoUseCase>((ref) {
  return TakePhotoUseCase(ref.watch(captureRepositoryProvider));
});

final pickCaptureImageUseCaseProvider = Provider<PickCaptureImageUseCase>((
  ref,
) {
  return PickCaptureImageUseCase(ref.watch(captureRepositoryProvider));
});

final analyzeCaptureUseCaseProvider = Provider<AnalyzeCaptureUseCase>((ref) {
  return AnalyzeCaptureUseCase(ref.watch(captureRepositoryProvider));
});

final saveAnalyzedCaptureUseCaseProvider = Provider<SaveAnalyzedCaptureUseCase>(
  (ref) {
    return SaveAnalyzedCaptureUseCase(ref.watch(captureRepositoryProvider));
  },
);
