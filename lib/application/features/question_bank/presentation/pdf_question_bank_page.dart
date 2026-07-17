import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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

    return Scaffold(
      appBar: AppBar(title: const Text('导入 PDF 题集')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          AppAsyncPrimaryButton(
            label: state.preview == null ? '选择 PDF 文件' : '重新选择 PDF',
            expanded: true,
            isLoading: state.phase == QuestionBankPhase.selecting ||
                state.phase == QuestionBankPhase.analyzing,
            onPressed: state.isBusy ? null : controller.pickAndAnalyzePdf,
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
          if (state.preview != null) ...[
            const SizedBox(height: AppSpacing.xLarge),
            _PreviewSummary(preview: state.preview!, state: state),
            const SizedBox(height: AppSpacing.medium),
            for (var index = 0;
                index < state.preview!.questions.length;
                index++) ...[
              _QuestionReviewCard(
                index: index,
                question: state.preview!.questions[index],
                included: !state.excludedQuestionIds
                    .contains(state.preview!.questions[index].id),
                onChanged: (included) => controller.toggleQuestion(
                  state.preview!.questions[index].id,
                  included,
                ),
              ),
              const SizedBox(height: AppSpacing.small),
            ],
            const SizedBox(height: AppSpacing.medium),
            AppAsyncPrimaryButton(
              label: '确认导入 ${state.includedQuestions.length} 道题',
              expanded: true,
              isLoading: state.phase == QuestionBankPhase.saving,
              onPressed: state.includedQuestions.isEmpty || state.isBusy
                  ? null
                  : controller.confirmImport,
              icon: const Icon(Icons.save_outlined),
            ),
            const SizedBox(height: AppSpacing.small),
            AppDefaultButton(
              label: '取消本次导入',
              expanded: true,
              onPressed: state.isBusy ? null : controller.reset,
              icon: const Icon(Icons.close),
            ),
          ] else ...[
            const SizedBox(height: AppSpacing.xLarge),
            Text('已导入题集', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.medium),
            if (state.sets.isEmpty)
              const AppEmptyView(
                title: '暂无 PDF 题集',
                message: '选择带文字层或清晰扫描页的 PDF，系统会拆题后等待确认。',
                icon: Icons.menu_book_outlined,
              )
            else
              for (final set in state.sets) ...[
                _QuestionSetCard(set: set),
                const SizedBox(height: AppSpacing.small),
              ],
          ],
        ],
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
            child: Text('答案：${question.answer ?? '未识别'}'),
          ),
          if (question.analysis?.isNotEmpty == true) ...[
            const SizedBox(height: AppSpacing.small),
            Align(
              alignment: Alignment.centerLeft,
              child: Text('解析：${question.analysis}'),
            ),
          ],
        ],
      ),
    );
  }
}

class _QuestionSetCard extends StatelessWidget {
  const _QuestionSetCard({required this.set});

  final PdfQuestionSet set;

  @override
  Widget build(BuildContext context) {
    return Card.filled(
      child: ExpansionTile(
        leading: const Icon(Icons.menu_book_outlined),
        title: Text(set.name),
        subtitle: Text('${set.questions.length} 道题 · ${_date(set.importedAt)}'),
        children: [
          for (var index = 0; index < set.questions.length; index++)
            ListTile(
              dense: true,
              title: Text(
                '${index + 1}. ${set.questions[index].stem}',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              subtitle: set.questions[index].answer == null
                  ? const Text('暂无参考答案')
                  : Text('答案：${set.questions[index].answer}'),
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
