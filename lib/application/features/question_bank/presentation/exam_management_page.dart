import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_session_card.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

enum _ExamAction { rename, delete }

class ExamManagementPage extends ConsumerWidget {
  const ExamManagementPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(questionBankControllerProvider);
    final controller = ref.read(questionBankControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('管理考卷')),
      body: !state.sessionsLoaded
          ? const AppLoadingView(message: '正在加载考卷')
          : state.sessions.isEmpty
              ? const AppEmptyView(
                  title: '暂无已生成考卷',
                  message: '返回 PDF 题库选择题型和数量后生成考卷。',
                  icon: Icons.assignment_outlined,
                )
              : ListView.separated(
                  padding: const EdgeInsets.all(AppSpacing.page),
                  itemCount: state.sessions.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: AppSpacing.small),
                  itemBuilder: (context, index) {
                    final session = state.sessions[index];
                    return ExamSessionCard(
                      session: session,
                      onOpen: () => context.go(
                        '/capture/pdf-import/exam/${session.id}',
                      ),
                      trailing: PopupMenuButton<_ExamAction>(
                        key: ValueKey('exam-menu-${session.id}'),
                        enabled: !state.isBusy,
                        tooltip: '管理考卷',
                        icon: const Icon(Icons.more_vert),
                        onSelected: (action) => switch (action) {
                          _ExamAction.rename =>
                            _renameExam(context, ref, controller, session),
                          _ExamAction.delete =>
                            _deleteExam(context, ref, controller, session),
                        },
                        itemBuilder: (context) => [
                          PopupMenuItem(
                            key: ValueKey('rename-exam-${session.id}'),
                            value: _ExamAction.rename,
                            child: const Row(
                              children: [
                                Icon(Icons.edit_outlined),
                                SizedBox(width: AppSpacing.medium),
                                Text('重命名'),
                              ],
                            ),
                          ),
                          PopupMenuItem(
                            key: ValueKey('delete-exam-${session.id}'),
                            value: _ExamAction.delete,
                            child: const Row(
                              children: [
                                Icon(Icons.delete_outline),
                                SizedBox(width: AppSpacing.medium),
                                Text('删除'),
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
    );
  }

  Future<void> _renameExam(
    BuildContext context,
    WidgetRef ref,
    QuestionBankController controller,
    ExamSession session,
  ) async {
    final title = await _showRenameDialog(context, session.title);
    if (!context.mounted || title == null || title == session.title) return;
    final renamed = await controller.renameExamSession(session.id, title);
    if (!context.mounted) return;
    if (renamed) {
      showAppSuccessSnackBar(context, '考卷已重命名');
    } else {
      showAppErrorSnackBar(
        context,
        ref.read(questionBankControllerProvider).errorMessage ?? '重命名失败',
      );
    }
  }

  Future<void> _deleteExam(
    BuildContext context,
    WidgetRef ref,
    QuestionBankController controller,
    ExamSession session,
  ) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除考卷',
      message: '确定删除“${session.title}”？作答记录也会一并删除。',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed || !context.mounted) return;
    final deleted = await controller.deleteExamSession(session.id);
    if (!context.mounted) return;
    if (deleted) {
      showAppSuccessSnackBar(context, '考卷已删除');
    } else {
      showAppErrorSnackBar(
        context,
        ref.read(questionBankControllerProvider).errorMessage ?? '删除失败',
      );
    }
  }

  Future<String?> _showRenameDialog(
    BuildContext context,
    String initialTitle,
  ) {
    return showDialog<String>(
      context: context,
      builder: (context) => _RenameExamDialog(initialTitle: initialTitle),
    );
  }
}

class _RenameExamDialog extends StatefulWidget {
  const _RenameExamDialog({required this.initialTitle});

  final String initialTitle;

  @override
  State<_RenameExamDialog> createState() => _RenameExamDialogState();
}

class _RenameExamDialogState extends State<_RenameExamDialog> {
  late final TextEditingController _textController;

  @override
  void initState() {
    super.initState();
    _textController = TextEditingController(text: widget.initialTitle);
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  void _submit() {
    final title = _textController.text.trim();
    if (title.isNotEmpty) Navigator.of(context).pop(title);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('重命名考卷'),
      content: TextField(
        key: const Key('exam-title-input'),
        controller: _textController,
        autofocus: true,
        maxLength: 40,
        textInputAction: TextInputAction.done,
        decoration: const InputDecoration(
          labelText: '考卷名称',
          prefixIcon: Icon(Icons.edit_outlined),
        ),
        onSubmitted: (_) => _submit(),
      ),
      actions: [
        AppDefaultButton(
          label: '取消',
          onPressed: () => Navigator.of(context).pop(),
        ),
        ValueListenableBuilder<TextEditingValue>(
          valueListenable: _textController,
          builder: (context, value, _) => AppPrimaryButton(
            key: const Key('confirm-exam-rename'),
            label: '保存',
            onPressed: value.text.trim().isEmpty ? null : _submit,
          ),
        ),
      ],
    );
  }
}
