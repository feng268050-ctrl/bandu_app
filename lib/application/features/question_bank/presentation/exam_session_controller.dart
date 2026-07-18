import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

enum ExamSessionPhase { idle, loading, ready, submitting, failed }

class ExamSessionUiState {
  const ExamSessionUiState({
    this.phase = ExamSessionPhase.idle,
    this.session,
    this.currentAttemptIndex = 0,
    this.answerInput = '',
    this.errorMessage,
  });

  final ExamSessionPhase phase;
  final ExamSession? session;
  final int currentAttemptIndex;
  final String answerInput;
  final String? errorMessage;

  bool get isBusy =>
      phase == ExamSessionPhase.loading || phase == ExamSessionPhase.submitting;

  ExamAttempt? get currentAttempt {
    final attempts = session?.attempts ?? const <ExamAttempt>[];
    if (currentAttemptIndex < 0 || currentAttemptIndex >= attempts.length) {
      return null;
    }
    return attempts[currentAttemptIndex];
  }

  ExamSessionUiState copyWith({
    ExamSessionPhase? phase,
    ExamSession? session,
    int? currentAttemptIndex,
    String? answerInput,
    String? errorMessage,
    bool clearError = false,
  }) {
    return ExamSessionUiState(
      phase: phase ?? this.phase,
      session: session ?? this.session,
      currentAttemptIndex: currentAttemptIndex ?? this.currentAttemptIndex,
      answerInput: answerInput ?? this.answerInput,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

final examSessionControllerProvider =
    NotifierProvider<ExamSessionController, ExamSessionUiState>(
  ExamSessionController.new,
);

class ExamSessionController extends Notifier<ExamSessionUiState> {
  @override
  ExamSessionUiState build() => const ExamSessionUiState();

  Future<void> load(String sessionId) async {
    state = const ExamSessionUiState(phase: ExamSessionPhase.loading);
    try {
      final session =
          await ref.read(loadExamSessionUseCaseProvider).call(sessionId);
      if (session == null || session.attempts.isEmpty) {
        state = const ExamSessionUiState(
          phase: ExamSessionPhase.failed,
          errorMessage: '考卷不存在或没有题目',
        );
        return;
      }
      final firstOpenIndex = session.attempts.indexWhere(
        (attempt) => !attempt.answerRevealed,
      );
      final index = firstOpenIndex < 0 ? 0 : firstOpenIndex;
      state = ExamSessionUiState(
        phase: ExamSessionPhase.ready,
        session: session,
        currentAttemptIndex: index,
        answerInput: session.attempts[index].userAnswer ?? '',
      );
      ref
          .read(questionBankControllerProvider.notifier)
          .upsertExamSession(session);
    } catch (error) {
      state = ExamSessionUiState(
        phase: ExamSessionPhase.failed,
        errorMessage: appFailureUserMessage(error),
      );
    }
  }

  void updateAnswer(String answer) {
    if (state.currentAttempt?.answerRevealed == true) return;
    state = state.copyWith(answerInput: answer, clearError: true);
  }

  Future<void> submitCurrentAnswer() async {
    final session = state.session;
    final attempt = state.currentAttempt;
    final answer = state.answerInput.trim();
    if (session == null || attempt == null || state.isBusy) return;
    if (answer.isEmpty) {
      state = state.copyWith(errorMessage: '请先填写答案');
      return;
    }

    state = state.copyWith(
      phase: ExamSessionPhase.submitting,
      clearError: true,
    );
    try {
      final updated = await ref.read(submitExamAnswerUseCaseProvider).call(
            sessionId: session.id,
            attemptId: attempt.id,
            userAnswer: answer,
          );
      state = state.copyWith(
        phase: ExamSessionPhase.ready,
        session: updated,
        clearError: true,
      );
      ref
          .read(questionBankControllerProvider.notifier)
          .upsertExamSession(updated);
    } catch (error) {
      state = state.copyWith(
        phase: ExamSessionPhase.ready,
        errorMessage: appFailureUserMessage(error),
      );
    }
  }

  void previousAttempt() => _moveTo(state.currentAttemptIndex - 1);

  void nextAttempt() => _moveTo(state.currentAttemptIndex + 1);

  void _moveTo(int index) {
    final attempts = state.session?.attempts ?? const <ExamAttempt>[];
    if (index < 0 || index >= attempts.length) return;
    state = state.copyWith(
      currentAttemptIndex: index,
      answerInput: attempts[index].userAnswer ?? '',
      clearError: true,
    );
  }
}
