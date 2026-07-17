import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/presentation/tutor_controller.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/media/local_file_image.dart';
import 'package:bandu_wrong_notebook/components/surfaces/app_message_surface.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class TutorPage extends ConsumerStatefulWidget {
  const TutorPage({super.key});

  @override
  ConsumerState<TutorPage> createState() => _TutorPageState();
}

class _TutorPageState extends ConsumerState<TutorPage> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _textController = TextEditingController();
  final _scrollController = ScrollController();

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
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
      endDrawer: _QuestionPickerDrawer(
        onSelected: (selection) => _applyQuestionSelection(
          selection,
          controller,
        ),
      ),
      appBar: AppBar(
        leading: IconButton(
          tooltip: '历史对话',
          onPressed: () => _scaffoldKey.currentState?.openDrawer(),
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
      body: Column(
        children: [
          if (state.activeSession != null)
            _TutorSessionModelBanner(
              modelName: state.resolvedModel?.displayName ??
                  state.selectedModel?.name ??
                  state.activeSession!.modelId ??
                  'Auto',
              fallbackOccurred: state.resolvedModel?.fallbackOccurred ?? false,
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
            onPickImage: controller.pickImage,
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
    );
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

class _ModelSelector extends StatelessWidget {
  const _ModelSelector({required this.state, required this.controller});

  final TutorUiState state;
  final TutorController controller;

  @override
  Widget build(BuildContext context) {
    final selected = state.selectedModel;
    return PopupMenuButton<String>(
      tooltip: '切换 AI 模型',
      initialValue: state.selectedModelId,
      enabled: state.models.isNotEmpty && !state.isSending,
      onSelected: controller.selectModel,
      itemBuilder: (context) => state.models
          .map(
            (model) => PopupMenuItem(
              value: model.id,
              child: Row(
                children: [
                  if (model.id == state.selectedModelId)
                    const Icon(Icons.check, size: 20)
                  else
                    const SizedBox(width: 20),
                  const SizedBox(width: AppSpacing.small),
                  Expanded(child: Text(model.name)),
                ],
              ),
            ),
          )
          .toList(),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 210),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Flexible(
              child: Text(
                selected?.name ?? (state.isLoading ? '正在读取模型' : '未配置模型'),
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
    required this.onRemoveImage,
    required this.onRemoveQuestion,
    required this.onToggleSpeech,
    required this.onSend,
  });

  final TextEditingController textController;
  final TutorUiState state;
  final VoidCallback onPickImage;
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
          AppSizes.primaryNavigationBodyOverlap + AppSpacing.small,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (state.questionContext != null || state.imagePath != null)
              Align(
                alignment: Alignment.centerLeft,
                child: Wrap(
                  spacing: AppSpacing.small,
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
                      InputChip(
                        avatar: const Icon(Icons.image_outlined, size: 18),
                        label: const Text('图片'),
                        onDeleted: onRemoveImage,
                      ),
                  ],
                ),
              ),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                IconButton(
                  tooltip: '导入图片',
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

class _HistoryDrawer extends ConsumerWidget {
  const _HistoryDrawer();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(tutorControllerProvider);
    final controller = ref.read(tutorControllerProvider.notifier);
    return Drawer(
      width: _drawerWidth(context),
      semanticLabel: '历史对话',
      child: SafeArea(
        child: Column(
          children: [
            ListTile(
              title:
                  Text('历史对话', style: Theme.of(context).textTheme.titleLarge),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton.filledTonal(
                    tooltip: '新建对话',
                    onPressed: () {
                      controller.newConversation();
                      Navigator.of(context).pop();
                    },
                    icon: const Icon(Icons.add),
                  ),
                  IconButton(
                    tooltip: '关闭历史对话',
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: state.sessions.isEmpty
                  ? const Center(child: Text('暂无历史对话'))
                  : ListView.builder(
                      itemCount: state.sessions.length,
                      itemBuilder: (context, index) {
                        final session = state.sessions[index];
                        return ListTile(
                          selected: session.id == state.activeSessionId,
                          leading: const Icon(Icons.chat_bubble_outline),
                          title: Text(session.title),
                          subtitle: Text('${session.messages.length} 条消息'),
                          onTap: () {
                            controller.openSession(session.id);
                            Navigator.of(context).pop();
                          },
                          trailing: IconButton(
                            tooltip: '删除对话',
                            onPressed: () =>
                                controller.deleteSession(session.id),
                            icon: const Icon(Icons.delete_outline),
                          ),
                        );
                      },
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
