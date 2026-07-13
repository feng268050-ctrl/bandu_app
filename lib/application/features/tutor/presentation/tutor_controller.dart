import 'dart:async';

import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/tutor_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class TutorUiState {
  const TutorUiState({
    this.models = const [],
    this.sessions = const [],
    this.isLoading = true,
    this.isSending = false,
    this.isListening = false,
    this.voiceTranscript = '',
    this.activeSessionId,
    this.selectedModelId,
    this.questionContext,
    this.imagePath,
    this.errorMessage,
  });

  final List<TutorModel> models;
  final List<TutorSession> sessions;
  final bool isLoading;
  final bool isSending;
  final bool isListening;
  final String voiceTranscript;
  final String? activeSessionId;
  final String? selectedModelId;
  final TutorQuestionContext? questionContext;
  final String? imagePath;
  final String? errorMessage;

  TutorSession? get activeSession {
    for (final session in sessions) {
      if (session.id == activeSessionId) return session;
    }
    return null;
  }

  TutorModel? get selectedModel {
    for (final model in models) {
      if (model.id == selectedModelId) return model;
    }
    return models.isEmpty ? null : models.first;
  }

  TutorUiState copyWith({
    List<TutorModel>? models,
    List<TutorSession>? sessions,
    bool? isLoading,
    bool? isSending,
    bool? isListening,
    String? voiceTranscript,
    String? activeSessionId,
    bool clearActiveSession = false,
    String? selectedModelId,
    bool clearSelectedModel = false,
    TutorQuestionContext? questionContext,
    bool clearQuestionContext = false,
    String? imagePath,
    bool clearImage = false,
    String? errorMessage,
    bool clearError = false,
  }) {
    return TutorUiState(
      models: models ?? this.models,
      sessions: sessions ?? this.sessions,
      isLoading: isLoading ?? this.isLoading,
      isSending: isSending ?? this.isSending,
      isListening: isListening ?? this.isListening,
      voiceTranscript: voiceTranscript ?? this.voiceTranscript,
      activeSessionId:
          clearActiveSession ? null : activeSessionId ?? this.activeSessionId,
      selectedModelId:
          clearSelectedModel ? null : selectedModelId ?? this.selectedModelId,
      questionContext:
          clearQuestionContext ? null : questionContext ?? this.questionContext,
      imagePath: clearImage ? null : imagePath ?? this.imagePath,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

final tutorControllerProvider = NotifierProvider<TutorController, TutorUiState>(
  TutorController.new,
);

class TutorController extends Notifier<TutorUiState> {
  StreamSubscription<String>? _speechSubscription;
  String _speechPrefix = '';

  @override
  TutorUiState build() {
    ref.onDispose(() {
      _speechSubscription?.cancel();
    });
    unawaited(_load());
    return const TutorUiState();
  }

  Future<void> refreshModels() async {
    try {
      final models = await ref.read(loadTutorModelsUseCaseProvider).call();
      state = state.copyWith(
        models: models,
        selectedModelId: _resolveModelId(models, state.selectedModelId),
        clearError: true,
      );
    } catch (error) {
      state = state.copyWith(errorMessage: _message(error));
    }
  }

  void selectModel(String modelId) {
    state = state.copyWith(selectedModelId: modelId, clearError: true);
    final session = state.activeSession;
    if (session != null) {
      final updated =
          session.copyWith(modelId: modelId, updatedAt: DateTime.now());
      _replaceSession(updated);
      unawaited(ref.read(saveTutorSessionUseCaseProvider).call(updated));
    }
  }

  void openSession(String sessionId) {
    final session =
        state.sessions.where((item) => item.id == sessionId).firstOrNull;
    if (session == null) return;
    state = state.copyWith(
      activeSessionId: sessionId,
      selectedModelId: _resolveModelId(state.models, session.modelId),
      clearQuestionContext: true,
      clearImage: true,
      clearError: true,
    );
  }

  void newConversation() {
    state = state.copyWith(
      clearActiveSession: true,
      clearQuestionContext: true,
      clearImage: true,
      clearError: true,
    );
  }

  Future<void> deleteSession(String sessionId) async {
    await ref.read(deleteTutorSessionUseCaseProvider).call(sessionId);
    final sessions =
        state.sessions.where((item) => item.id != sessionId).toList();
    state = state.copyWith(
      sessions: sessions,
      activeSessionId:
          state.activeSessionId == sessionId ? sessions.firstOrNull?.id : null,
      clearActiveSession:
          state.activeSessionId == sessionId && sessions.isEmpty,
    );
  }

  void selectQuestion(TutorQuestionContext context) {
    state = state.copyWith(questionContext: context, clearError: true);
  }

  void clearQuestion() {
    state = state.copyWith(clearQuestionContext: true);
  }

  Future<void> pickImage() async {
    try {
      final path = await ref.read(pickTutorImageUseCaseProvider).call();
      if (path != null) {
        state = state.copyWith(imagePath: path, clearError: true);
      }
    } catch (error) {
      state = state.copyWith(errorMessage: _message(error));
    }
  }

  void clearImage() {
    state = state.copyWith(clearImage: true);
  }

  Future<void> sendMessage(String draft) async {
    if (state.isSending) return;
    final message = draft.trim().isNotEmpty
        ? draft.trim()
        : state.questionContext != null
            ? '请分步骤讲解这道题。'
            : state.imagePath != null
                ? '请识别并讲解这张图片中的题目。'
                : '';
    if (message.isEmpty) return;
    if (state.models.isEmpty) {
      state = state.copyWith(errorMessage: '未读取到可用模型，请先在“我的 > AI 配置”中新增模型。');
      return;
    }

    final now = DateTime.now();
    final current = state.activeSession;
    final session = current ??
        TutorSession(
          id: now.microsecondsSinceEpoch.toString(),
          title: _titleFrom(message, state.questionContext),
          modelId: state.selectedModelId,
          messages: const [],
          createdAt: now,
          updatedAt: now,
        );
    final history = session.messages;
    final userMessage = TutorMessage(
      id: '${now.microsecondsSinceEpoch}-user',
      role: TutorMessageRole.user,
      content: message,
      createdAt: now,
      imagePath: state.imagePath,
      questionContext: state.questionContext,
    );
    final pendingSession = session.copyWith(
      modelId: state.selectedModelId,
      messages: [...history, userMessage],
      updatedAt: now,
    );
    _replaceSession(pendingSession);
    state = state.copyWith(
      activeSessionId: pendingSession.id,
      isSending: true,
      clearQuestionContext: true,
      clearImage: true,
      clearError: true,
    );
    await ref.read(saveTutorSessionUseCaseProvider).call(pendingSession);

    try {
      final reply = await ref.read(sendTutorMessageUseCaseProvider).call(
            message: message,
            history: history,
            modelId: state.selectedModelId,
            questionContext: userMessage.questionContext,
            imagePath: userMessage.imagePath,
          );
      final assistant = TutorMessage(
        id: reply.id,
        role: TutorMessageRole.assistant,
        content: reply.content,
        createdAt: reply.createdAt,
      );
      final completed = pendingSession.copyWith(
        modelId: reply.modelId,
        messages: [...pendingSession.messages, assistant],
        updatedAt: reply.createdAt,
      );
      _replaceSession(completed);
      state = state.copyWith(
        isSending: false,
        selectedModelId: reply.modelId,
      );
      await ref.read(saveTutorSessionUseCaseProvider).call(completed);
    } catch (error) {
      state = state.copyWith(
        isSending: false,
        errorMessage: _message(error),
      );
    }
  }

  Future<void> startListening(String existingDraft) async {
    if (state.isListening) {
      await stopListening();
      return;
    }
    try {
      _speechPrefix = existingDraft.trim();
      final stream = await ref.read(startSpeechInputUseCaseProvider).call();
      state = state.copyWith(
        isListening: true,
        voiceTranscript: _speechPrefix,
        clearError: true,
      );
      await _speechSubscription?.cancel();
      _speechSubscription = stream.listen(
        (recognized) {
          final text = recognized.trim();
          state = state.copyWith(
            voiceTranscript: [
              if (_speechPrefix.isNotEmpty) _speechPrefix,
              if (text.isNotEmpty) text,
            ].join(_speechPrefix.isNotEmpty && text.isNotEmpty ? ' ' : ''),
          );
        },
        onError: (Object error) {
          state = state.copyWith(
            isListening: false,
            errorMessage: _message(error),
          );
        },
      );
    } catch (error) {
      state = state.copyWith(
        isListening: false,
        errorMessage: _message(error),
      );
    }
  }

  Future<void> stopListening() async {
    await ref.read(stopSpeechInputUseCaseProvider).call();
    await _speechSubscription?.cancel();
    _speechSubscription = null;
    state = state.copyWith(isListening: false);
  }

  Future<void> _load() async {
    List<TutorSession> sessions = const [];
    List<TutorModel> models = const [];
    String? errorMessage;
    try {
      sessions = await ref.read(loadTutorSessionsUseCaseProvider).call();
    } catch (error) {
      errorMessage = _message(error);
    }
    try {
      models = await ref.read(loadTutorModelsUseCaseProvider).call();
    } catch (error) {
      errorMessage = _message(error);
    }
    final active = sessions.firstOrNull;
    state = state.copyWith(
      sessions: sessions,
      models: models,
      activeSessionId: active?.id,
      clearActiveSession: active == null,
      selectedModelId: _resolveModelId(models, active?.modelId),
      clearSelectedModel: models.isEmpty,
      isLoading: false,
      errorMessage: errorMessage,
      clearError: errorMessage == null,
    );
  }

  void _replaceSession(TutorSession session) {
    final sessions = [
      session,
      ...state.sessions.where((item) => item.id != session.id),
    ];
    state = state.copyWith(sessions: sessions, activeSessionId: session.id);
  }

  String? _resolveModelId(List<TutorModel> models, String? preferred) {
    if (models.any((model) => model.id == preferred)) return preferred;
    for (final model in models) {
      if (model.isDefault) return model.id;
    }
    return models.firstOrNull?.id;
  }

  String _titleFrom(String message, TutorQuestionContext? context) {
    final source = context?.title.trim().isNotEmpty == true
        ? context!.title.trim()
        : message;
    return source.length > 24 ? '${source.substring(0, 24)}...' : source;
  }

  String _message(Object error) {
    final message = error.toString();
    return message
        .replaceFirst('Exception: ', '')
        .replaceFirst('Bad state: ', '');
  }
}
