import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_labels.dart';
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

enum _QuestionSetAction { rename, delete }

class QuestionSetManagementPage extends ConsumerWidget {
  const QuestionSetManagementPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(questionBankControllerProvider);
    final controller = ref.read(questionBankControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('管理题集')),
      body: !state.setsLoaded
          ? const AppLoadingView(message: '正在加载题集')
          : state.sets.isEmpty
              ? const AppEmptyView(
                  title: '暂无已导入 PDF',
                  message: '返回 PDF 题库选择文件导入后，可在此重命名或删除。',
                  icon: Icons.picture_as_pdf_outlined,
                )
              : ListView.separated(
                  padding: const EdgeInsets.all(AppSpacing.page),
                  itemCount: state.sets.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: AppSpacing.small),
                  itemBuilder: (context, index) {
                    final set = state.sets[index];
                    final relatedExamCount = state.sessions
                        .where((session) => session.questionSetId == set.id)
                        .length;
                    return Card.filled(
                      child: ListTile(
                        leading: const Icon(Icons.menu_book_outlined),
                        title: Text(set.name),
                        subtitle: Text(
                          '${set.questions.length} 道题 · '
                          '${bankQuestionTypeSummary(set.questionTypeCounts, includeTotal: false)}'
                          '${relatedExamCount > 0 ? '\n关联考卷 $relatedExamCount 份' : ''}',
                        ),
                        isThreeLine: relatedExamCount > 0,
                        onTap: () => context.go(
                          '/capture/pdf-import/bank/${set.id}',
                        ),
                        trailing: PopupMenuButton<_QuestionSetAction>(
                          key: ValueKey('question-set-menu-${set.id}'),
                          enabled: !state.isBusy,
                          tooltip: '管理题集',
                          icon: const Icon(Icons.more_vert),
                          onSelected: (action) => switch (action) {
                            _QuestionSetAction.rename => _renameQuestionSet(
                                context,
                                ref,
                                controller,
                                set,
                              ),
                            _QuestionSetAction.delete => _deleteQuestionSet(
                                context,
                                ref,
                                controller,
                                set,
                                relatedExamCount,
                              ),
                          },
                          itemBuilder: (context) => [
                            PopupMenuItem(
                              key: ValueKey('rename-question-set-${set.id}'),
                              value: _QuestionSetAction.rename,
                              child: const Row(
                                children: [
                                  Icon(Icons.edit_outlined),
                                  SizedBox(width: AppSpacing.medium),
                                  Text('重命名'),
                                ],
                              ),
                            ),
                            PopupMenuItem(
                              key: ValueKey('delete-question-set-${set.id}'),
                              value: _QuestionSetAction.delete,
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
                      ),
                    );
                  },
                ),
    );
  }

  Future<void> _renameQuestionSet(
    BuildContext context,
    WidgetRef ref,
    QuestionBankController controller,
    PdfQuestionSet set,
  ) async {
    final name = await _showRenameDialog(context, set.name);
    if (!context.mounted || name == null || name == set.name) return;
    final renamed = await controller.renameQuestionSet(set.id, name);
    if (!context.mounted) return;
    if (renamed) {
      showAppSuccessSnackBar(context, '题集已重命名');
    } else {
      showAppErrorSnackBar(
        context,
        ref.read(questionBankControllerProvider).errorMessage ?? '重命名失败',
      );
    }
  }

  Future<void> _deleteQuestionSet(
    BuildContext context,
    WidgetRef ref,
    QuestionBankController controller,
    PdfQuestionSet set,
    int relatedExamCount,
  ) async {
    final examHint = relatedExamCount > 0
        ? '关联的 $relatedExamCount 份考卷也会一并删除。'
        : '本地 PDF 副本也会一并删除。';
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除题集',
      message: '确定删除“${set.name}”？$examHint',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed || !context.mounted) return;
    final deleted = await controller.deleteQuestionSet(set.id);
    if (!context.mounted) return;
    if (deleted) {
      showAppSuccessSnackBar(context, '题集已删除');
    } else {
      showAppErrorSnackBar(
        context,
        ref.read(questionBankControllerProvider).errorMessage ?? '删除失败',
      );
    }
  }

  Future<String?> _showRenameDialog(
    BuildContext context,
    String initialName,
  ) {
    return showDialog<String>(
      context: context,
      builder: (context) => _RenameQuestionSetDialog(initialName: initialName),
    );
  }
}

class _RenameQuestionSetDialog extends StatefulWidget {
  const _RenameQuestionSetDialog({required this.initialName});

  final String initialName;

  @override
  State<_RenameQuestionSetDialog> createState() =>
      _RenameQuestionSetDialogState();
}

class _RenameQuestionSetDialogState extends State<_RenameQuestionSetDialog> {
  late final TextEditingController _textController;

  @override
  void initState() {
    super.initState();
    _textController = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  void _submit() {
    final name = _textController.text.trim();
    if (name.isNotEmpty) Navigator.of(context).pop(name);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('重命名题集'),
      content: TextField(
        key: const Key('question-set-name-input'),
        controller: _textController,
        autofocus: true,
        maxLength: 40,
        textInputAction: TextInputAction.done,
        decoration: const InputDecoration(
          labelText: '题集名称',
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
            key: const Key('confirm-question-set-rename'),
            label: '保存',
            onPressed: value.text.trim().isEmpty ? null : _submit,
          ),
        ),
      ],
    );
  }
}
