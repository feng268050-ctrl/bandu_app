import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_model_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/presentation/tutor_controller.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_radius.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_shapes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/interaction/app_horizontal_swipe_region.dart';
import 'package:bandu_wrong_notebook/components/media/local_file_image.dart';
import 'package:bandu_wrong_notebook/components/media/tutor_image_editor_page.dart';
import 'package:bandu_wrong_notebook/components/surfaces/app_message_surface.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

const _tutorComposerBottomPadding =
    AppSizes.primaryNavigationBodyOverlap + AppSpacing.small;

class TutorPage extends ConsumerStatefulWidget {
  const TutorPage({super.key});

  @override
  ConsumerState<TutorPage> createState() => _TutorPageState();
}

class _TutorPageState extends ConsumerState<TutorPage> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _textController = TextEditingController();
  final _scrollController = ScrollController();
  GoRouter? _router;

  static const _openHistorySwipeVelocity = 280.0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final router = GoRouter.of(context);
    if (!identical(_router, router)) {
      _router?.routerDelegate.removeListener(_onRouteChanged);
      _router = router;
      _router!.routerDelegate.addListener(_onRouteChanged);
    }
  }

  @override
  void dispose() {
    _router?.routerDelegate.removeListener(_onRouteChanged);
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _onRouteChanged() {
    if (!mounted) return;
    final path = _router?.state.uri.path ?? '';
    if (path == '/tutor' || path.startsWith('/tutor/')) return;
    _closeOpenDrawers();
  }

  void _closeOpenDrawers() {
    final scaffold = _scaffoldKey.currentState;
    if (scaffold == null) return;
    if (scaffold.isDrawerOpen) {
      scaffold.closeDrawer();
    }
    if (scaffold.isEndDrawerOpen) {
      scaffold.closeEndDrawer();
    }
  }

  void _openHistoryDrawer() {
    final scaffold = _scaffoldKey.currentState;
    if (scaffold == null || scaffold.isDrawerOpen || scaffold.isEndDrawerOpen) {
      return;
    }
    scaffold.openDrawer();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(tutorControllerProvider);
    final controller = ref.read(tutorControllerProvider.notifier);
    ref.listen(tutorControllerProvider, (previous, next) {
      if (next.voiceTranscript != previous?.voiceTranscript &&
          next.isListening) {
        _textController.value = TextEditingValue(
          text: next.voiceTranscript,
          selection:
              TextSelection.collapsed(offset: next.voiceTranscript.length),
        );
      }
      final previousCount = previous?.activeSession?.messages.length ?? 0;
      final nextCount = next.activeSession?.messages.length ?? 0;
      if (nextCount != previousCount) {
        WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToBottom());
      }
    });

    return Scaffold(
      key: _scaffoldKey,
      drawer: const _HistoryDrawer(),
      drawerEdgeDragWidth: MediaQuery.sizeOf(context).width * 0.35,
      endDrawer: _QuestionPickerDrawer(
        onSelected: (selection) => _applyQuestionSelection(
          selection,
          controller,
        ),
      ),
      appBar: AppBar(
        leading: IconButton(
          tooltip: '历史对话',
          onPressed: _openHistoryDrawer,
          icon: const Icon(Icons.menu),
        ),
        title: _ModelSelector(state: state, controller: controller),
        actions: [
          IconButton(
            tooltip: '从应用中选择题目',
            onPressed: () => _scaffoldKey.currentState?.openEndDrawer(),
            icon: const Icon(Icons.post_add_outlined),
          ),
        ],
      ),
      body: AppHorizontalSwipeRegion(
        minimumVelocity: _openHistorySwipeVelocity,
        onSwipeRight: _openHistoryDrawer,
        child: Column(
          children: [
            if (state.activeSession != null)
              _TutorSessionModelBanner(
                modelName: state.resolvedModel?.displayName ??
                    state.selectedModel?.name ??
                    state.activeSession!.modelId ??
                    'Auto',
                fallbackOccurred:
                    state.resolvedModel?.fallbackOccurred ?? false,
              ),
            Expanded(
              child: _ConversationView(
                controller: _scrollController,
                messages: state.activeSession?.messages ?? const [],
                isSending: state.isSending,
                onChooseQuestion: () =>
                    _scaffoldKey.currentState?.openEndDrawer(),
              ),
            ),
            if (state.errorMessage != null)
              _InlineError(
                message: state.errorMessage!,
                onRetry: state.models.isEmpty ? controller.refreshModels : null,
              ),
            _TutorComposer(
              textController: _textController,
              state: state,
              onPickImage: () => _pickTutorImage(context, controller),
              onEditImage: state.imagePath == null
                  ? null
                  : () => _editTutorImage(
                        context,
                        controller,
                        state.imagePath!,
                      ),
              onRemoveImage: controller.clearImage,
              onRemoveQuestion: controller.clearQuestion,
              onToggleSpeech: () => controller.startListening(
                _textController.text,
              ),
              onSend: () async {
                final text = _textController.text;
                _textController.clear();
                await controller.stopListening();
                await controller.sendMessage(text);
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _pickTutorImage(
    BuildContext context,
    TutorController controller,
  ) async {
    final source = await showModalBottomSheet<TutorImageSource>(
      context: context,
      useRootNavigator: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt_outlined),
              title: const Text('拍照'),
              onTap: () => Navigator.of(context).pop(TutorImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('从相册选择'),
              onTap: () => Navigator.of(context).pop(TutorImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (source == null || !context.mounted) return;
    final path = await controller.pickImage(source);
    if (path == null || !context.mounted) return;
    await _editTutorImage(context, controller, path, required: false);
  }

  Future<void> _editTutorImage(
    BuildContext context,
    TutorController controller,
    String imagePath, {
    bool required = false,
  }) async {
    final edited = await Navigator.of(context, rootNavigator: true).push<String>(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (context) => TutorImageEditorPage(imagePath: imagePath),
      ),
    );
    if (!context.mounted) return;
    if (edited != null) {
      controller.setImagePath(edited);
    } else if (required) {
      controller.clearImage();
    }
  }

  Future<void> _applyQuestionSelection(
    _QuestionSelection selection,
    TutorController controller,
  ) async {
    if (selection.bankQuestion != null) {
      final question = selection.bankQuestion!;
      controller.selectQuestion(
        TutorQuestionContext(
          id: question.id,
          title: question.stem,
          content: question.tutorContext,
          source: 'PDF 题集',
        ),
      );
      return;
    }
    final item = selection.errorItem;
    if (item == null) return;
    try {
      final detail =
          await ref.read(fetchErrorItemDetailUseCaseProvider).call(item.id);
      controller.selectQuestion(
        TutorQuestionContext(
          id: detail.id,
          title: detail.title,
          content: [
            detail.questionText ?? detail.title,
            if (detail.answer?.isNotEmpty == true) '参考答案：${detail.answer}',
            if (detail.analysis?.isNotEmpty == true) '已有解析：${detail.analysis}',
          ].join('\n'),
          source: '错题本',
        ),
      );
    } catch (_) {
      controller.selectQuestion(
        TutorQuestionContext(
          id: item.id,
          title: item.title,
          content: item.title,
          source: '错题本',
        ),
      );
    }
  }

  void _scrollToBottom() {
    if (!_scrollController.hasClients) return;
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent,
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOut,
    );
  }
}

class _TutorSessionModelBanner extends StatelessWidget {
  const _TutorSessionModelBanner({
    required this.modelName,
    required this.fallbackOccurred,
  });

  final String modelName;
  final bool fallbackOccurred;

  @override
  Widget build(BuildContext context) => Material(
        color: Theme.of(context).colorScheme.secondaryContainer,
        child: ListTile(
          dense: true,
          leading: const Icon(Icons.lock_outline),
          title: Text('本会话固定使用：$modelName'),
          subtitle: fallbackOccurred ? const Text('主模型不可用，已切换到备用模型') : null,
        ),
      );
}

class _ModelSelector extends ConsumerWidget {
  const _ModelSelector({required this.state, required this.controller});

  static const _autoValue = '__auto__';

  final TutorUiState state;
  final TutorController controller;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final preferences = ref.watch(aiPurposePreferencesControllerProvider);
    final tutorPreference = preferences.maybeWhen(
      data: (values) =>
          values.where((item) => item.purpose == AiPurpose.tutor).firstOrNull,
      orElse: () => null,
    );
    final isAuto = tutorPreference?.mode != AiSelectionMode.manual;
    final selected = state.selectedModel;
    final label = isAuto
        ? 'Auto'
        : (selected?.listLabel ?? (state.isLoading ? '正在读取模型' : '未配置模型'));
    final colorScheme = Theme.of(context).colorScheme;

    Future<void> savePreference(AiPurposePreference preference) async {
      await ref
          .read(aiPurposePreferencesControllerProvider.notifier)
          .save(preference);
    }

    Future<void> setAuto(bool enabled) async {
      final current = tutorPreference ??
          const AiPurposePreference(purpose: AiPurpose.tutor);
      if (enabled) {
        await savePreference(
          AiPurposePreference(
            purpose: AiPurpose.tutor,
            allowUserModelsInAuto: current.allowUserModelsInAuto,
            allowFallbackInManual: current.allowFallbackInManual,
          ),
        );
        return;
      }
      final modelId = state.selectedModelId ?? state.models.firstOrNull?.id;
      if (modelId == null) return;
      await savePreference(
        AiPurposePreference(
          purpose: AiPurpose.tutor,
          mode: AiSelectionMode.manual,
          selectedModelId: modelId,
          allowUserModelsInAuto: current.allowUserModelsInAuto,
          allowFallbackInManual: current.allowFallbackInManual,
        ),
      );
      controller.selectModel(modelId);
    }

    Future<void> selectModel(String modelId) async {
      final current = tutorPreference ??
          const AiPurposePreference(purpose: AiPurpose.tutor);
      await savePreference(
        AiPurposePreference(
          purpose: AiPurpose.tutor,
          mode: AiSelectionMode.manual,
          selectedModelId: modelId,
          allowUserModelsInAuto: current.allowUserModelsInAuto,
          allowFallbackInManual: current.allowFallbackInManual,
        ),
      );
      controller.selectModel(modelId);
    }

    return PopupMenuButton<String>(
      tooltip: '切换 AI 模型',
      enabled: !state.isSending,
      onSelected: (value) async {
        if (value == _autoValue) {
          await setAuto(true);
          return;
        }
        await selectModel(value);
      },
      itemBuilder: (context) => [
        PopupMenuItem<String>(
          value: _autoValue,
          child: Row(
            children: [
              Icon(
                Icons.auto_awesome,
                size: 20,
                color: isAuto ? colorScheme.primary : null,
              ),
              const SizedBox(width: AppSpacing.small),
              const Expanded(
                child: Text(
                  'Auto',
                  style: TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
              // Absorb pointer so toggling the switch doesn't also select Auto.
              InkWell(
                onTap: () {},
                child: Switch.adaptive(
                  value: isAuto,
                  onChanged: state.isSending
                      ? null
                      : (value) async {
                          Navigator.of(context).pop();
                          await setAuto(value);
                        },
                ),
              ),
            ],
          ),
        ),
        if (!isAuto) ...[
          const PopupMenuDivider(),
          ...state.models.map(
            (model) => PopupMenuItem<String>(
              value: model.id,
              child: Row(
                children: [
                  if (model.id == state.selectedModelId)
                    Icon(Icons.check, size: 20, color: colorScheme.primary)
                  else
                    const SizedBox(width: 20),
                  const SizedBox(width: AppSpacing.small),
                  Expanded(child: Text(model.listLabel)),
                ],
              ),
            ),
          ),
        ],
      ],
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 210),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isAuto) ...[
              Icon(Icons.auto_awesome, size: 18, color: colorScheme.primary),
              const SizedBox(width: AppSpacing.xSmall),
            ],
            Flexible(
              child: Text(
                label,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}

class _ConversationView extends StatelessWidget {
  const _ConversationView({
    required this.controller,
    required this.messages,
    required this.isSending,
    required this.onChooseQuestion,
  });

  final ScrollController controller;
  final List<TutorMessage> messages;
  final bool isSending;
  final VoidCallback onChooseQuestion;

  @override
  Widget build(BuildContext context) {
    if (messages.isEmpty) {
      return AppEmptyView(
        title: '开始 AI 辅导',
        message: '直接输入问题，或从错题本和 PDF 题集中选择一道题。',
        icon: Icons.forum_outlined,
        actionLabel: '选择题目',
        onAction: onChooseQuestion,
      );
    }
    return ListView.builder(
      controller: controller,
      padding: const EdgeInsets.all(AppSpacing.page),
      itemCount: messages.length + (isSending ? 1 : 0),
      itemBuilder: (context, index) {
        if (index == messages.length) {
          return const _ThinkingIndicator();
        }
        return Padding(
          padding: const EdgeInsets.only(bottom: AppSpacing.medium),
          child: _MessageBubble(message: messages[index]),
        );
      },
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});
  final TutorMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == TutorMessageRole.user;
    final colorScheme = Theme.of(context).colorScheme;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.84,
        ),
        child: AppMessageSurface(
          emphasized: isUser,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (message.questionContext != null) ...[
                Text(
                  '${message.questionContext!.source} · ${message.questionContext!.title}',
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: colorScheme.primary,
                      ),
                ),
                const Divider(),
              ],
              if (message.imagePath != null) ...[
                LocalFileImage(
                  path: message.imagePath!,
                  width: 180,
                  height: 120,
                  borderRadius: 6,
                ),
                const SizedBox(height: AppSpacing.small),
              ],
              SelectableText(message.content),
            ],
          ),
        ),
      ),
    );
  }
}

class _ThinkingIndicator extends StatelessWidget {
  const _ThinkingIndicator();

  @override
  Widget build(BuildContext context) {
    return const Align(
      alignment: Alignment.centerLeft,
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: AppSpacing.small),
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      ),
    );
  }
}

class _TutorComposer extends StatelessWidget {
  const _TutorComposer({
    required this.textController,
    required this.state,
    required this.onPickImage,
    required this.onEditImage,
    required this.onRemoveImage,
    required this.onRemoveQuestion,
    required this.onToggleSpeech,
    required this.onSend,
  });

  final TextEditingController textController;
  final TutorUiState state;
  final VoidCallback onPickImage;
  final VoidCallback? onEditImage;
  final VoidCallback onRemoveImage;
  final VoidCallback onRemoveQuestion;
  final VoidCallback onToggleSpeech;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Material(
      color: colorScheme.surfaceContainer,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.medium,
          AppSpacing.small,
          AppSpacing.medium,
          _tutorComposerBottomPadding,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (state.questionContext != null || state.imagePath != null)
              Align(
                alignment: Alignment.centerLeft,
                child: Wrap(
                  spacing: AppSpacing.small,
                  runSpacing: AppSpacing.small,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    if (state.questionContext != null)
                      InputChip(
                        avatar:
                            const Icon(Icons.description_outlined, size: 18),
                        label: Text(
                          state.questionContext!.title,
                          overflow: TextOverflow.ellipsis,
                        ),
                        onDeleted: onRemoveQuestion,
                      ),
                    if (state.imagePath != null)
                      _ComposerImagePreview(
                        path: state.imagePath!,
                        onTap: onEditImage,
                        onRemove: onRemoveImage,
                      ),
                  ],
                ),
              ),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                IconButton(
                  tooltip: '拍照或选图',
                  onPressed: state.isSending ? null : onPickImage,
                  icon: const Icon(Icons.add),
                ),
                Expanded(
                  child: TextField(
                    controller: textController,
                    minLines: 1,
                    maxLines: 5,
                    textInputAction: TextInputAction.newline,
                    decoration: const InputDecoration(
                      hintText: '问问 AI 辅导老师',
                      border: InputBorder.none,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: state.isListening ? '停止语音输入' : '语音输入',
                  onPressed: state.isSending ? null : onToggleSpeech,
                  color: state.isListening ? colorScheme.error : null,
                  icon: Icon(
                      state.isListening ? Icons.stop_circle : Icons.mic_none),
                ),
                IconButton.filled(
                  tooltip: '发送',
                  onPressed: state.isSending ? null : onSend,
                  icon: const Icon(Icons.arrow_upward),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ComposerImagePreview extends StatelessWidget {
  const _ComposerImagePreview({
    required this.path,
    required this.onRemove,
    this.onTap,
  });

  final String path;
  final VoidCallback? onTap;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Material(
          color: colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(AppRadius.small),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: onTap,
            child: Stack(
              children: [
                LocalFileImage(
                  path: path,
                  width: 72,
                  height: 72,
                  borderRadius: AppRadius.small,
                ),
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 0,
                  child: ColoredBox(
                    color: Colors.black54,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        vertical: 2,
                        horizontal: 4,
                      ),
                      child: Text(
                        onTap == null ? '图片' : '编辑',
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: Colors.white,
                            ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        Positioned(
          top: -8,
          right: -8,
          child: Material(
            color: colorScheme.surface,
            shape: const CircleBorder(),
            elevation: 1,
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onRemove,
              child: const Padding(
                padding: EdgeInsets.all(4),
                child: Icon(Icons.close, size: 16),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message, this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Material(
      color: colorScheme.errorContainer,
      child: ListTile(
        dense: true,
        leading: Icon(Icons.error_outline, color: colorScheme.onErrorContainer),
        title: Text(
          message,
          style: TextStyle(color: colorScheme.onErrorContainer),
        ),
        trailing: onRetry == null
            ? null
            : TextButton(onPressed: onRetry, child: const Text('重试')),
      ),
    );
  }
}

class _HistoryDrawer extends ConsumerStatefulWidget {
  const _HistoryDrawer();

  @override
  ConsumerState<_HistoryDrawer> createState() => _HistoryDrawerState();
}

class _HistoryDrawerState extends ConsumerState<_HistoryDrawer> {
  final _searchController = TextEditingController();
  final _searchFocus = FocusNode();
  var _searching = false;
  var _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocus.dispose();
    super.dispose();
  }

  void _toggleSearch({required bool enabled}) {
    setState(() {
      _searching = enabled;
      if (!enabled) {
        _query = '';
        _searchController.clear();
      }
    });
    if (enabled) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _searchFocus.requestFocus();
      });
    } else {
      _searchFocus.unfocus();
    }
  }

  List<TutorSession> _filteredSessions(List<TutorSession> sessions) {
    final query = _query.trim().toLowerCase();
    if (query.isEmpty) return sessions;
    return sessions
        .where((session) => session.title.toLowerCase().contains(query))
        .toList(growable: false);
  }

  Future<void> _confirmDelete(
    TutorController controller,
    TutorSession session,
  ) async {
    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除对话'),
        content: Text('确定删除「${session.title}」？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (shouldDelete == true) {
      await controller.deleteSession(session.id);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(tutorControllerProvider);
    final controller = ref.read(tutorControllerProvider.notifier);
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final sessions = _filteredSessions(state.sessions);

    return Drawer(
      width: _drawerWidth(context),
      semanticLabel: '历史对话',
      child: SafeArea(
        bottom: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.page,
                AppSpacing.small,
                AppSpacing.small,
                AppSpacing.small,
              ),
              child: _searching
                  ? TextField(
                      controller: _searchController,
                      focusNode: _searchFocus,
                      onChanged: (value) => setState(() => _query = value),
                      textInputAction: TextInputAction.search,
                      decoration: InputDecoration(
                        hintText: '搜索对话',
                        prefixIcon: const Icon(Icons.search),
                        suffixIcon: IconButton(
                          tooltip: '取消搜索',
                          onPressed: () => _toggleSearch(enabled: false),
                          icon: const Icon(Icons.close),
                        ),
                        border: OutlineInputBorder(
                          borderRadius: AppShapes.searchFieldBorderRadius,
                        ),
                        isDense: true,
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: AppSpacing.large,
                          vertical: AppSpacing.medium,
                        ),
                      ),
                    )
                  : Row(
                      children: [
                        Expanded(
                          child: Text(
                            '历史对话',
                            style: textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        IconButton.filledTonal(
                          tooltip: '搜索对话',
                          onPressed: () => _toggleSearch(enabled: true),
                          style: IconButton.styleFrom(
                            shape: const CircleBorder(),
                          ),
                          icon: const Icon(Icons.search),
                        ),
                      ],
                    ),
            ),
            Expanded(
              child: sessions.isEmpty
                  ? Center(
                      child: Text(
                        state.sessions.isEmpty ? '暂无历史对话' : '没有匹配的对话',
                        style: textTheme.bodyMedium?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    )
                  : ListView(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppSpacing.small,
                      ),
                      children: [
                        Padding(
                          padding: const EdgeInsets.fromLTRB(
                            AppSpacing.medium,
                            AppSpacing.medium,
                            AppSpacing.medium,
                            AppSpacing.small,
                          ),
                          child: Text(
                            '最近',
                            style: textTheme.labelLarge?.copyWith(
                              color: colorScheme.onSurfaceVariant,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        for (final session in sessions)
                          ListTile(
                            selected: session.id == state.activeSessionId,
                            shape: AppShapes.roundedListTile,
                            title: Text(
                              session.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            onTap: () {
                              controller.openSession(session.id);
                              Navigator.of(context).pop();
                            },
                            onLongPress: () =>
                                _confirmDelete(controller, session),
                          ),
                      ],
                    ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.medium,
                AppSpacing.small,
                AppSpacing.medium,
                _tutorComposerBottomPadding,
              ),
              child: FilledButton.icon(
                onPressed: () {
                  controller.newConversation();
                  Navigator.of(context).pop();
                },
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(
                    AppSizes.minTouchTarget,
                  ),
                  shape: const StadiumBorder(),
                ),
                icon: const Icon(Icons.edit_square),
                label: const Text('聊天'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuestionSelection {
  const _QuestionSelection({this.errorItem, this.bankQuestion});
  final ErrorItemSummary? errorItem;
  final BankQuestion? bankQuestion;
}

class _QuestionPickerDrawer extends ConsumerWidget {
  const _QuestionPickerDrawer({required this.onSelected});

  final ValueChanged<_QuestionSelection> onSelected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final library = ref.watch(libraryControllerProvider);
    final libraryItems = library.valueOrNull ?? const <ErrorItemSummary>[];
    final questionBanks = ref.watch(questionBankControllerProvider).sets;
    final bankQuestions = [
      for (final bank in questionBanks)
        for (final question in bank.questions) question,
    ];
    return Drawer(
      width: _drawerWidth(context),
      semanticLabel: '从应用中选择题目',
      child: SafeArea(
        child: Column(
          children: [
            ListTile(
              title:
                  Text('选择题目', style: Theme.of(context).textTheme.titleLarge),
              subtitle: const Text('错题本与 PDF 题集'),
              trailing: IconButton(
                tooltip: '关闭题目选择',
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close),
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: ListView(
                children: [
                  const ListTile(title: Text('错题本')),
                  if (library.isLoading)
                    const ListTile(
                      leading: SizedBox.square(
                        dimension: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                      title: Text('正在读取错题'),
                    )
                  else if (library.hasError)
                    const ListTile(
                      leading: Icon(Icons.error_outline),
                      title: Text('错题读取失败'),
                    )
                  else if (libraryItems.isEmpty)
                    const ListTile(title: Text('暂无错题'))
                  else
                    for (final item in libraryItems)
                      ListTile(
                        leading: const Icon(Icons.library_books_outlined),
                        title: Text(item.title),
                        subtitle: Text(item.subjectName),
                        onTap: () => _closeAndSelect(
                          context,
                          _QuestionSelection(errorItem: item),
                        ),
                      ),
                  const Divider(),
                  const ListTile(title: Text('PDF 题集')),
                  if (bankQuestions.isEmpty)
                    const ListTile(title: Text('暂无已导入题目'))
                  else
                    for (final question in bankQuestions)
                      ListTile(
                        leading: const Icon(Icons.picture_as_pdf_outlined),
                        title: Text(
                          question.stem,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        onTap: () => _closeAndSelect(
                          context,
                          _QuestionSelection(bankQuestion: question),
                        ),
                      ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _closeAndSelect(
    BuildContext context,
    _QuestionSelection selection,
  ) {
    Navigator.of(context).pop();
    onSelected(selection);
  }
}

double _drawerWidth(BuildContext context) {
  return (MediaQuery.sizeOf(context).width * 0.88).clamp(0.0, 420.0).toDouble();
}
