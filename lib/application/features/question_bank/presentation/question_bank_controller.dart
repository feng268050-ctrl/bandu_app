import 'dart:async';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

enum QuestionBankPhase {
  idle,
  selecting,
  analyzing,
  preview,
  saving,
  creatingExam,
  managingExam,
  saved,
  failed
}

class QuestionBankUiState {
  const QuestionBankUiState({
    this.phase = QuestionBankPhase.idle,
    this.sets = const [],
    this.sessions = const [],
    this.setsLoaded = false,
    this.sessionsLoaded = false,
    this.preview,
    this.excludedQuestionIds = const {},
    this.errorMessage,
  });

  final QuestionBankPhase phase;
  final List<PdfQuestionSet> sets;
  final List<ExamSession> sessions;
  final bool setsLoaded;
  final bool sessionsLoaded;
  final PdfImportPreview? preview;
  final Set<String> excludedQuestionIds;
  final String? errorMessage;

  bool get isBusy =>
      phase == QuestionBankPhase.selecting ||
      phase == QuestionBankPhase.analyzing ||
      phase == QuestionBankPhase.saving ||
      phase == QuestionBankPhase.creatingExam ||
      phase == QuestionBankPhase.managingExam;

  List<BankQuestion> get includedQuestions =>
      preview?.questions
          .where((question) => !excludedQuestionIds.contains(question.id))
          .toList() ??
      const [];

  QuestionBankUiState copyWith({
    QuestionBankPhase? phase,
    List<PdfQuestionSet>? sets,
    List<ExamSession>? sessions,
    bool? setsLoaded,
    bool? sessionsLoaded,
    PdfImportPreview? preview,
    bool clearPreview = false,
    Set<String>? excludedQuestionIds,
    String? errorMessage,
    bool clearError = false,
  }) {
    return QuestionBankUiState(
      phase: phase ?? this.phase,
      sets: sets ?? this.sets,
      sessions: sessions ?? this.sessions,
      setsLoaded: setsLoaded ?? this.setsLoaded,
      sessionsLoaded: sessionsLoaded ?? this.sessionsLoaded,
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
    unawaited(_loadExamSessions());
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
        errorMessage: appFailureUserMessage(error),
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
        setsLoaded: true,
        clearPreview: true,
        excludedQuestionIds: const {},
      );
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: appFailureUserMessage(error),
      );
    }
  }

  Future<ExamSession?> createExam(
    PdfQuestionSet questionSet,
    Map<BankQuestionType, int> questionCounts,
  ) async {
    if (state.isBusy) return null;
    state = state.copyWith(
      phase: QuestionBankPhase.creatingExam,
      clearError: true,
    );
    try {
      final session = await ref.read(createRandomExamUseCaseProvider).call(
            questionSetId: questionSet.id,
            questionCounts: questionCounts,
          );
      state = state.copyWith(
        phase: QuestionBankPhase.idle,
        sessions: [
          session,
          ...state.sessions.where((item) => item.id != session.id),
        ],
        sessionsLoaded: true,
        clearError: true,
      );
      return session;
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: _examErrorMessage(error),
      );
      return null;
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

  void upsertExamSession(ExamSession session) {
    state = state.copyWith(
      sessions: [
        session,
        ...state.sessions.where((item) => item.id != session.id),
      ],
      sessionsLoaded: true,
    );
  }

  Future<bool> renameExamSession(String sessionId, String title) async {
    final normalizedTitle = title.trim();
    if (state.isBusy || normalizedTitle.isEmpty) return false;
    state = state.copyWith(
      phase: QuestionBankPhase.managingExam,
      clearError: true,
    );
    try {
      final updated = await ref.read(renameExamSessionUseCaseProvider).call(
            sessionId: sessionId,
            title: normalizedTitle,
          );
      state = state.copyWith(
        phase: QuestionBankPhase.idle,
        sessions: [
          for (final session in state.sessions)
            if (session.id == updated.id) updated else session,
        ],
        clearError: true,
      );
      return true;
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: _examErrorMessage(error),
      );
      return false;
    }
  }

  Future<bool> deleteExamSession(String sessionId) async {
    if (state.isBusy) return false;
    state = state.copyWith(
      phase: QuestionBankPhase.managingExam,
      clearError: true,
    );
    try {
      await ref.read(deleteExamSessionUseCaseProvider).call(sessionId);
      state = state.copyWith(
        phase: QuestionBankPhase.idle,
        sessions:
            state.sessions.where((session) => session.id != sessionId).toList(),
        clearError: true,
      );
      return true;
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: _examErrorMessage(error),
      );
      return false;
    }
  }

  Future<bool> renameQuestionSet(String questionSetId, String name) async {
    final normalizedName = name.trim();
    if (state.isBusy || normalizedName.isEmpty) return false;
    state = state.copyWith(
      phase: QuestionBankPhase.managingExam,
      clearError: true,
    );
    try {
      final updated = await ref.read(renamePdfQuestionSetUseCaseProvider).call(
            questionSetId: questionSetId,
            name: normalizedName,
          );
      state = state.copyWith(
        phase: QuestionBankPhase.idle,
        sets: [
          for (final set in state.sets)
            if (set.id == updated.id) updated else set,
        ],
        clearError: true,
      );
      return true;
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: _examErrorMessage(error),
      );
      return false;
    }
  }

  Future<bool> deleteQuestionSet(String questionSetId) async {
    if (state.isBusy) return false;
    state = state.copyWith(
      phase: QuestionBankPhase.managingExam,
      clearError: true,
    );
    try {
      await ref.read(deletePdfQuestionSetUseCaseProvider).call(questionSetId);
      state = state.copyWith(
        phase: QuestionBankPhase.idle,
        sets: state.sets.where((set) => set.id != questionSetId).toList(),
        sessions: state.sessions
            .where((session) => session.questionSetId != questionSetId)
            .toList(),
        clearError: true,
      );
      return true;
    } catch (error) {
      state = state.copyWith(
        phase: QuestionBankPhase.failed,
        errorMessage: _examErrorMessage(error),
      );
      return false;
    }
  }

  Future<void> _loadSets() async {
    try {
      final sets = await ref.read(loadPdfQuestionSetsUseCaseProvider).call();
      state = state.copyWith(sets: sets, setsLoaded: true);
    } catch (_) {
      // Existing imports are optional context; a damaged cache must not block import.
      state = state.copyWith(setsLoaded: true);
    }
  }

  Future<void> _loadExamSessions() async {
    try {
      final sessions = await ref.read(loadExamSessionsUseCaseProvider).call();
      state = state.copyWith(sessions: sessions, sessionsLoaded: true);
    } catch (_) {
      // A damaged exam cache must not block importing or viewing question sets.
      state = state.copyWith(sessionsLoaded: true);
    }
  }
}

String _examErrorMessage(Object error) {
  final message = error.toString();
  if (message.contains('question_type_count_not_enough')) {
    return '某题型的题数不足，请调整各题型数量';
  }
  if (message.contains('question_bank_not_found')) {
    return '题库不存在，请返回后重试';
  }
  if (message.contains('exam_session_not_found')) {
    return '考卷不存在或已被删除';
  }
  return appFailureUserMessage(error);
}
