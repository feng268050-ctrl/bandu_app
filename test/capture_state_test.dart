import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('initial capture state cannot analyze', () {
    final state = CaptureUiState.initial();

    expect(state.phase, CapturePhase.idle);
    expect(state.canAnalyze, isFalse);
  });

  test('preview capture state can analyze', () {
    const state = CaptureUiState(
      phase: CapturePhase.preview,
      localImagePath: '/tmp/original.jpg',
    );

    expect(state.canAnalyze, isTrue);
  });

  test('failed analysis can retry while failed save can retry save', () {
    const failedAnalysis = CaptureUiState(
      phase: CapturePhase.failed,
      localImagePath: '/tmp/original.jpg',
      errorMessage: 'offline',
    );
    const failedSave = CaptureUiState(
      phase: CapturePhase.failed,
      localImagePath: '/tmp/original.jpg',
      result: AnalyzeResult(title: '题目'),
      errorMessage: 'offline',
    );

    expect(failedAnalysis.canAnalyze, isTrue);
    expect(failedAnalysis.canSave, isFalse);
    expect(failedSave.canAnalyze, isFalse);
    expect(failedSave.canSave, isTrue);
  });
}
