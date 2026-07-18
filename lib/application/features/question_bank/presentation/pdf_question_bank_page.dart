import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_session_card.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_labels.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:bandu_wrong_notebook/components/forms/app_number_stepper.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class PdfQuestionBankPage extends ConsumerWidget {
  const PdfQuestionBankPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(questionBankControllerProvider);
    final controller = ref.read(questionBankControllerProvider.notifier);
    ref.listen(questionBankControllerProvider, (previous, next) {
      if (next.phase == QuestionBankPhase.saved &&
          previous?.phase != QuestionBankPhase.saved) {
        showAppSuccessSnackBar(context, 'PDF 题集已保存');
      }
    });

    final preview = state.preview;

    return Scaffold(
      appBar: AppBar(title: const Text('导入 PDF 题集')),
      body: preview == null
          ? _QuestionBankOverview(
              state: state,
              onPickPdf: controller.pickAndAnalyzePdf,
              onGenerateExam: (set) => _openGenerateExam(context, ref, set),
              onOpenQuestionSet: (set) =>
                  context.go('/capture/pdf-import/bank/${set.id}'),
              onOpenExam: (session) =>
                  context.go('/capture/pdf-import/exam/${session.id}'),
              onManageExams: () => context.go('/capture/pdf-import/exams'),
              onManageQuestionSets: () =>
                  context.go('/capture/pdf-import/banks'),
            )
          : _PdfReviewList(
              state: state,
              preview: preview,
              onPickPdf: controller.pickAndAnalyzePdf,
              onToggleQuestion: controller.toggleQuestion,
            ),
      bottomNavigationBar: preview == null
          ? null
          : _ImportActionBar(
              includedCount: state.includedQuestions.length,
              totalCount: preview.questions.length,
              isSaving: state.phase == QuestionBankPhase.saving,
              onConfirm: state.includedQuestions.isEmpty || state.isBusy
                  ? null
                  : controller.confirmImport,
              onCancel: state.isBusy ? null : controller.reset,
            ),
    );
  }

  Future<void> _openGenerateExam(
    BuildContext context,
    WidgetRef ref,
    PdfQuestionSet questionSet,
  ) async {
    final session = await showDialog<ExamSession>(
      context: context,
      builder: (context) => _GenerateExamDialog(questionSet: questionSet),
    );
    if (session != null && context.mounted) {
      context.go('/capture/pdf-import/exam/${session.id}');
    }
  }
}

class _QuestionBankOverview extends StatelessWidget {
  const _QuestionBankOverview({
    required this.state,
    required this.onPickPdf,
    required this.onGenerateExam,
    required this.onOpenQuestionSet,
    required this.onOpenExam,
    required this.onManageExams,
    required this.onManageQuestionSets,
  });

  final QuestionBankUiState state;
  final VoidCallback onPickPdf;
  final ValueChanged<PdfQuestionSet> onGenerateExam;
  final ValueChanged<PdfQuestionSet> onOpenQuestionSet;
  final ValueChanged<ExamSession> onOpenExam;
  final VoidCallback onManageExams;
  final VoidCallback onManageQuestionSets;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(AppSpacing.page),
      children: [
        AppAsyncPrimaryButton(
          label: '选择 PDF 文件',
          expanded: true,
          isLoading: state.phase == QuestionBankPhase.selecting ||
              state.phase == QuestionBankPhase.analyzing,
          onPressed: state.isBusy ? null : onPickPdf,
          icon: const Icon(Icons.picture_as_pdf_outlined),
        ),
        if (state.phase == QuestionBankPhase.analyzing) ...[
          const SizedBox(height: AppSpacing.large),
          const AppLoadingView(message: '正在提取页面并自动拆题'),
        ],
        if (state.errorMessage != null) ...[
          const SizedBox(height: AppSpacing.medium),
          _ImportError(message: state.errorMessage!),
        ],
        const SizedBox(height: AppSpacing.xLarge),
        if (state.sessions.isNotEmpty) ...[
          Row(
            children: [
              Expanded(
                child: Text(
                  '已生成考卷',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              TextButton.icon(
                key: const Key('manage-exams'),
                onPressed: onManageExams,
                icon: const Icon(Icons.edit_note_outlined),
                label: const Text('管理'),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.medium),
          for (final session in state.sessions) ...[
            ExamSessionCard(
              session: session,
              onOpen: () => onOpenExam(session),
            ),
            const SizedBox(height: AppSpacing.small),
          ],
          const SizedBox(height: AppSpacing.large),
        ],
        Row(
          children: [
            Expanded(
              child: Text(
                '已导入题集',
                style: Theme.of(context).textTheme.titleLarge,
              ),
            ),
            if (state.sets.isNotEmpty)
              TextButton.icon(
                key: const Key('manage-question-sets'),
                onPressed: onManageQuestionSets,
                icon: const Icon(Icons.edit_note_outlined),
                label: const Text('管理'),
              ),
          ],
        ),
        const SizedBox(height: AppSpacing.medium),
        if (state.sets.isEmpty)
          const AppEmptyView(
            title: '暂无 PDF 题集',
            message: '选择带文字层或清晰扫描页的 PDF，系统会拆题后等待确认。',
            icon: Icons.menu_book_outlined,
          )
        else
          for (final set in state.sets) ...[
            _QuestionSetCard(
              set: set,
              onGenerateExam: () => onGenerateExam(set),
              onOpenQuestions: () => onOpenQuestionSet(set),
            ),
            const SizedBox(height: AppSpacing.small),
          ],
      ],
    );
  }
}

class _PdfReviewList extends StatelessWidget {
  const _PdfReviewList({
    required this.state,
    required this.preview,
    required this.onPickPdf,
    required this.onToggleQuestion,
  });

  final QuestionBankUiState state;
  final PdfImportPreview preview;
  final VoidCallback onPickPdf;
  final void Function(String id, bool included) onToggleQuestion;

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.page,
            AppSpacing.page,
            AppSpacing.page,
            AppSpacing.medium,
          ),
          sliver: SliverList.list(
            children: [
              AppAsyncPrimaryButton(
                label: '重新选择 PDF',
                expanded: true,
                isLoading: false,
                onPressed: state.isBusy ? null : onPickPdf,
                icon: const Icon(Icons.picture_as_pdf_outlined),
              ),
              if (state.errorMessage != null) ...[
                const SizedBox(height: AppSpacing.medium),
                _ImportError(message: state.errorMessage!),
              ],
              const SizedBox(height: AppSpacing.medium),
              _PreviewSummary(preview: preview, state: state),
            ],
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.page),
          sliver: SliverList(
            delegate: SliverChildBuilderDelegate(
              (context, itemIndex) {
                if (itemIndex.isOdd) {
                  return const SizedBox(height: AppSpacing.small);
                }
                final questionIndex = itemIndex ~/ 2;
                final question = preview.questions[questionIndex];
                return _QuestionReviewCard(
                  index: questionIndex,
                  question: question,
                  included: !state.excludedQuestionIds.contains(question.id),
                  onChanged: (included) =>
                      onToggleQuestion(question.id, included),
                );
              },
              childCount: preview.questions.isEmpty
                  ? 0
                  : preview.questions.length * 2 - 1,
            ),
          ),
        ),
        const SliverToBoxAdapter(
          child: SizedBox(height: AppSpacing.medium),
        ),
      ],
    );
  }
}

class _ImportActionBar extends StatelessWidget {
  const _ImportActionBar({
    required this.includedCount,
    required this.totalCount,
    required this.isSaving,
    required this.onConfirm,
    required this.onCancel,
  });

  final int includedCount;
  final int totalCount;
  final bool isSaving;
  final VoidCallback? onConfirm;
  final VoidCallback? onCancel;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Material(
      color: colorScheme.surfaceContainer,
      elevation: 8,
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Divider(color: colorScheme.outlineVariant),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.page,
                AppSpacing.medium,
                AppSpacing.page,
                AppSpacing.page,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '已选择 $includedCount / $totalCount 道题',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.small),
                  AppAsyncPrimaryButton(
                    key: const Key('confirm-pdf-import'),
                    label: '确认导入 $includedCount 道题',
                    expanded: true,
                    isLoading: isSaving,
                    onPressed: onConfirm,
                    icon: const Icon(Icons.save_outlined),
                  ),
                  const SizedBox(height: AppSpacing.small),
                  AppDefaultButton(
                    key: const Key('cancel-pdf-import'),
                    label: '取消本次导入',
                    expanded: true,
                    onPressed: onCancel,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PreviewSummary extends StatelessWidget {
  const _PreviewSummary({required this.preview, required this.state});

  final PdfImportPreview preview;
  final QuestionBankUiState state;

  @override
  Widget build(BuildContext context) {
    return Card.outlined(
      child: ListTile(
        leading: const Icon(Icons.fact_check_outlined),
        title: Text(preview.fileName),
        subtitle: Text(
          '${preview.totalPages} 页 · ${preview.questions.length} 道题 · '
          '${preview.needsReviewCount} 道需复核'
          '${preview.resolvedModel == null ? '' : '\n${_modelLabel(preview.resolvedModel!)}'}',
        ),
        trailing: Text('${state.includedQuestions.length} 已选'),
      ),
    );
  }

  String _modelLabel(AiResolvedModel model) => model.fallbackOccurred
      ? '主模型不可用，已切换到备用模型：${model.displayName}'
      : '由 Auto 选择：${model.displayName}';
}

class _QuestionReviewCard extends StatelessWidget {
  const _QuestionReviewCard({
    required this.index,
    required this.question,
    required this.included,
    required this.onChanged,
  });

  final int index;
  final BankQuestion question;
  final bool included;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Card.filled(
      clipBehavior: Clip.antiAlias,
      child: ExpansionTile(
        leading: Checkbox(
          value: included,
          onChanged: (value) => onChanged(value ?? false),
        ),
        title: Text(
          '${index + 1}. ${question.stem}',
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Text(
          question.needsReview
              ? '需要复核${question.sourcePage == null ? '' : ' · 第 ${question.sourcePage} 页'}'
              : '已识别${question.sourcePage == null ? '' : ' · 第 ${question.sourcePage} 页'}',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: question.needsReview
                    ? colorScheme.tertiary
                    : colorScheme.onSurfaceVariant,
              ),
        ),
        childrenPadding: const EdgeInsets.fromLTRB(
          AppSpacing.large,
          0,
          AppSpacing.large,
          AppSpacing.large,
        ),
        children: [
          if (question.options.isNotEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: Text(question.options.join('\n')),
            ),
          const SizedBox(height: AppSpacing.small),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              '答案：${question.answer ?? '未识别'}',
              style: TextStyle(color: colorScheme.onSurfaceVariant),
            ),
          ),
          if (question.analysis?.isNotEmpty == true) ...[
            const SizedBox(height: AppSpacing.small),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                '解析：${question.analysis}',
                style: TextStyle(color: colorScheme.onSurfaceVariant),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _QuestionSetCard extends StatelessWidget {
  const _QuestionSetCard({
    required this.set,
    required this.onGenerateExam,
    required this.onOpenQuestions,
  });

  final PdfQuestionSet set;
  final VoidCallback onGenerateExam;
  final VoidCallback onOpenQuestions;

  @override
  Widget build(BuildContext context) {
    final typeSummary = bankQuestionTypeSummary(
      set.questionTypeCounts,
      includeTotal: false,
    );
    return Card.filled(
      child: Column(
        children: [
          ListTile(
            leading: const Icon(Icons.menu_book_outlined),
            title: Text(set.name),
            subtitle: Text(
              '${set.questions.length} 道题 · ${_date(set.importedAt)}\n'
              '$typeSummary',
            ),
            isThreeLine: true,
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.large),
            child: AppPrimaryButton(
              label: '生成随机考卷',
              expanded: true,
              onPressed: set.questions.isEmpty ? null : onGenerateExam,
              icon: const Icon(Icons.shuffle),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.list_alt_outlined),
            title: const Text('查看题目'),
            subtitle: Text('浏览全部 ${set.questions.length} 道题'),
            trailing: const Icon(Icons.chevron_right),
            onTap: onOpenQuestions,
          ),
        ],
      ),
    );
  }

  String _date(DateTime value) {
    final local = value.toLocal();
    return '${local.year}-${local.month.toString().padLeft(2, '0')}-'
        '${local.day.toString().padLeft(2, '0')}';
  }
}

class _GenerateExamDialog extends ConsumerStatefulWidget {
  const _GenerateExamDialog({required this.questionSet});

  final PdfQuestionSet questionSet;

  @override
  ConsumerState<_GenerateExamDialog> createState() =>
      _GenerateExamDialogState();
}

class _GenerateExamDialogState extends ConsumerState<_GenerateExamDialog> {
  late final Map<BankQuestionType, int> _availableCounts;
  late final Map<BankQuestionType, int> _selectedCounts;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _availableCounts = widget.questionSet.questionTypeCounts;
    _selectedCounts = _initialExamQuestionCounts(_availableCounts);
  }

  List<BankQuestionType> get _visibleQuestionTypes =>
      bankQuestionTypeDisplayOrder
          .where((type) => (_availableCounts[type] ?? 0) > 0)
          .toList();

  int get _selectedTotal =>
      _selectedCounts.values.fold<int>(0, (sum, count) => sum + count);

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(questionBankControllerProvider);
    final isCreating = state.phase == QuestionBankPhase.creatingExam;
    return AlertDialog(
      title: const Text('生成随机考卷'),
      content: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: 400,
          maxHeight: MediaQuery.sizeOf(context).height * 0.62,
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${widget.questionSet.name} · 共 ${widget.questionSet.questions.length} 道题',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
              ),
              const SizedBox(height: AppSpacing.small),
              for (var index = 0;
                  index < _visibleQuestionTypes.length;
                  index++) ...[
                AppNumberStepper(
                  label: bankQuestionTypeLabel(_visibleQuestionTypes[index]),
                  supportingText:
                      '可选 ${_availableCounts[_visibleQuestionTypes[index]]} 道',
                  value: _selectedCounts[_visibleQuestionTypes[index]] ?? 0,
                  minimum: 0,
                  maximum: _availableCounts[_visibleQuestionTypes[index]]!,
                  enabled: !isCreating,
                  decrementKey: Key(
                    'exam-count-decrement-${_visibleQuestionTypes[index].name}',
                  ),
                  valueKey: Key(
                    'exam-count-value-${_visibleQuestionTypes[index].name}',
                  ),
                  incrementKey: Key(
                    'exam-count-increment-${_visibleQuestionTypes[index].name}',
                  ),
                  onChanged: (value) => _updateCount(
                    _visibleQuestionTypes[index],
                    value,
                  ),
                ),
                if (index < _visibleQuestionTypes.length - 1)
                  const Divider(height: 1),
              ],
              const Divider(),
              ListTile(
                key: const Key('exam-total-count'),
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.functions),
                title: Text('共 $_selectedTotal 题'),
              ),
              if (_errorMessage != null || state.errorMessage != null) ...[
                const SizedBox(height: AppSpacing.small),
                Text(
                  _errorMessage ?? state.errorMessage!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.error,
                      ),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: isCreating ? null : () => Navigator.of(context).pop(),
          child: const Text('取消'),
        ),
        FilledButton.icon(
          key: const Key('create-random-exam'),
          onPressed: isCreating || _selectedTotal == 0 ? null : _createExam,
          icon: isCreating
              ? const SizedBox.square(
                  dimension: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.shuffle),
          label: Text(isCreating ? '生成中' : '开始答题'),
        ),
      ],
    );
  }

  void _updateCount(BankQuestionType type, int value) {
    setState(() {
      _selectedCounts[type] = value;
      _errorMessage = null;
    });
  }

  Future<void> _createExam() async {
    if (_selectedTotal <= 0) {
      setState(() => _errorMessage = '至少选择 1 道题');
      return;
    }
    setState(() => _errorMessage = null);

    final questionCounts = <BankQuestionType, int>{
      for (final type in _visibleQuestionTypes)
        if ((_selectedCounts[type] ?? 0) > 0) type: _selectedCounts[type]!,
    };
    final session = await ref
        .read(questionBankControllerProvider.notifier)
        .createExam(widget.questionSet, questionCounts);
    if (mounted && session != null) {
      Navigator.of(context).pop(session);
    }
  }
}

Map<BankQuestionType, int> _initialExamQuestionCounts(
  Map<BankQuestionType, int> availableCounts,
) {
  final selectedCounts = <BankQuestionType, int>{
    for (final type in bankQuestionTypeDisplayOrder) type: 0,
  };
  final totalAvailable =
      availableCounts.values.fold<int>(0, (sum, count) => sum + count);
  var remaining = totalAvailable < 10 ? totalAvailable : 10;
  while (remaining > 0) {
    var addedInRound = false;
    for (final type in bankQuestionTypeDisplayOrder) {
      final selected = selectedCounts[type]!;
      if (selected >= (availableCounts[type] ?? 0)) continue;
      selectedCounts[type] = selected + 1;
      remaining--;
      addedInRound = true;
      if (remaining == 0) break;
    }
    if (!addedInRound) break;
  }
  return selectedCounts;
}

class _ImportError extends StatelessWidget {
  const _ImportError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(Icons.error_outline, color: colorScheme.error),
        const SizedBox(width: AppSpacing.small),
        Expanded(
          child: Text(
            message,
            style: TextStyle(color: colorScheme.error),
          ),
        ),
      ],
    );
  }
}
