import 'package:bandu_wrong_notebook/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/features/practice/data/practice_api_service.dart';
import 'package:bandu_wrong_notebook/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/features/stats/data/stats_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceControllerProvider =
    NotifierProvider<PracticeController, PracticeUiState>(
  PracticeController.new,
);

class PracticeController extends Notifier<PracticeUiState> {
  static const _difficulty = 'medium';

  @override
  PracticeUiState build() {
    return const PracticeUiState();
  }

  Future<void> generateFromLatestError() async {
    final items = ref.read(libraryControllerProvider).valueOrNull ?? [];
    if (items.isEmpty) {
      state = state.copyWith(
        errorMessage: '错题本暂无错题，请先拍题并保存。',
        noticeMessage: null,
      );
      return;
    }

    state = state.copyWith(
      isBusy: true,
      showAnswer: false,
      errorMessage: null,
      noticeMessage: null,
    );
    try {
      final question = await ref.read(practiceApiServiceProvider).generate(
            errorItemId: items.first.id,
            difficulty: _difficulty,
          );
      state = PracticeUiState(question: question);
    } catch (error) {
      state = state.copyWith(
        isBusy: false,
        errorMessage: error.toString(),
        noticeMessage: null,
      );
    }
  }

  void toggleAnswer() {
    state = state.copyWith(showAnswer: !state.showAnswer);
  }

  Future<void> recordResult(bool isCorrect) async {
    final question = state.question;
    if (question == null) {
      return;
    }

    state = state.copyWith(isBusy: true, errorMessage: null, noticeMessage: null);
    try {
      await ref.read(practiceApiServiceProvider).record(
            subject: question.subjectName,
            difficulty: _difficulty,
            isCorrect: isCorrect,
          );
      ref.invalidate(statsOverviewProvider);
      state = state.copyWith(
        isBusy: false,
        noticeMessage: isCorrect ? '已记录：答对' : '已记录：答错',
        errorMessage: null,
      );
    } catch (error) {
      state = state.copyWith(
        isBusy: false,
        errorMessage: error.toString(),
        noticeMessage: null,
      );
    }
  }
}
