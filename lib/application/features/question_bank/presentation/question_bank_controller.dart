import 'dart:async';

import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

enum QuestionBankPhase {
  idle,
  selecting,
  analyzing,
  preview,
  saving,
  saved,
  failed
}

class QuestionBankUiState {
  const QuestionBankUiState({
    this.phase = QuestionBankPhase.idle,
    this.sets = const [],
    this.preview,
    this.excludedQuestionIds = const {},
    this.errorMessage,
  });

  final QuestionBankPhase phase;
  final List<PdfQuestionSet> sets;
  final PdfImportPreview? preview;
  final Set<String> excludedQuestionIds;
  final String? errorMessage;

  bool get isBusy =>
      phase == QuestionBankPhase.selecting ||
      phase == QuestionBankPhase.analyzing ||
      phase == QuestionBankPhase.saving;

  List<BankQuestion> get includedQuestions =>
      preview?.questions
          .where((question) => !excludedQuestionIds.contains(question.id))
          .toList() ??
      const [];

  QuestionBankUiState copyWith({
    QuestionBankPhase? phase,
    List<PdfQuestionSet>? sets,
    PdfImportPreview? preview,
    bool clearPreview = false,
    Set<String>? excludedQuestionIds,
    String? errorMessage,
    bool clearError = false,
  }) {
    return QuestionBankUiState(
      phase: phase ?? this.phase,
      sets: sets ?? this.sets,
      preview: clearPreview ? null : preview ?? this.preview,
      excludedQuestionIds: excludedQuestionIds ?? this.excludedQuestionIds,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

final questionBankControllerProvider =
    NotifierProvider<QuestionBankController, QuestionBankUiState>(
  QuestionBankController.new,
);

class QuestionBankController extends Notifier<QuestionBankUiState> {
  @override
  QuestionBankUiState build() {
    unawaited(_loadSets());
    return const QuestionBankUiState();
  }

  Future<void> pickAndAnalyzePdf() async {
    if (state.isBusy) return;
    state = state.copyWith(
      phase: QuestionBankPhase.selecting,
      clearPreview: true,
      excludedQuestionIds: const {},
      clearError: true,
    );
    try {
      final path = await ref.read(pickPdfQuestionSetUseCaseProvider).call();
      if (path == null) {
        state = state.copyWith(phase: QuestionBankPhase.idle);
        return;
      }
      state = state.copyWith(phase: QuestionBankPhase.analyzing);
      final preview =
          await ref.read(analyzePdfQuestionSetUseCaseProvider).call(path);
      if (preview.questions.isEmpty) {
        throw StateError('PDF 中没有识别到可导入题目');
      }
      state = state.copyWith(
        phase: QuestionBankPhase.preview,
        preview: preview,
        excludedQuestionIds: const {},
      );
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: error.toString(),
      );
    }
  }

  void toggleQuestion(String id, bool included) {
    final excluded = {...state.excludedQuestionIds};
    if (included) {
      excluded.remove(id);
    } else {
      excluded.add(id);
    }
    state = state.copyWith(excludedQuestionIds: excluded);
  }

  Future<void> confirmImport() async {
    final preview = state.preview;
    final questions = state.includedQuestions;
    if (preview == null || questions.isEmpty || state.isBusy) return;
    state = state.copyWith(phase: QuestionBankPhase.saving, clearError: true);
    try {
      final saved = await ref
          .read(savePdfQuestionSetUseCaseProvider)
          .call(preview.copyWith(questions: questions));
      state = state.copyWith(
        phase: QuestionBankPhase.saved,
        sets: [saved, ...state.sets.where((item) => item.id != saved.id)],
        clearPreview: true,
        excludedQuestionIds: const {},
      );
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: error.toString(),
      );
    }
  }

  void reset() {
    state = state.copyWith(
      phase: QuestionBankPhase.idle,
      clearPreview: true,
      excludedQuestionIds: const {},
      clearError: true,
    );
  }

  Future<void> _loadSets() async {
    try {
      final sets = await ref.read(loadPdfQuestionSetsUseCaseProvider).call();
      state = state.copyWith(sets: sets);
    } catch (_) {
      // Existing imports are optional context; a damaged cache must not block import.
    }
  }
}
