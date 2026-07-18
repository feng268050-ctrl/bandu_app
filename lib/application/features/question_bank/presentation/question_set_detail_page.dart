import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_labels.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class QuestionSetDetailPage extends ConsumerWidget {
  const QuestionSetDetailPage({required this.questionSetId, super.key});

  final String questionSetId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(questionBankControllerProvider);
    final questionSet =
        state.sets.where((set) => set.id == questionSetId).firstOrNull;

    return Scaffold(
      appBar: AppBar(title: const Text('题目列表')),
      body: !state.setsLoaded
          ? const AppLoadingView(message: '正在加载题集')
          : questionSet == null
              ? const AppErrorView(
                  title: '题集不存在',
                  message: '该题集可能已被删除，请返回题库列表。',
                )
              : _QuestionList(questionSet: questionSet),
    );
  }
}

class _QuestionList extends StatelessWidget {
  const _QuestionList({required this.questionSet});

  final PdfQuestionSet questionSet;

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
          sliver: SliverToBoxAdapter(
            child: Card.outlined(
              child: ListTile(
                leading: const Icon(Icons.menu_book_outlined),
                title: Text(questionSet.name),
                subtitle: Text(
                  '${questionSet.questions.length} 道题 · 来源：${questionSet.sourceFileName}',
                ),
              ),
            ),
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
                return _QuestionCard(
                  index: questionIndex,
                  question: questionSet.questions[questionIndex],
                );
              },
              childCount: questionSet.questions.isEmpty
                  ? 0
                  : questionSet.questions.length * 2 - 1,
            ),
          ),
        ),
        const SliverToBoxAdapter(
          child: SizedBox(height: AppSpacing.page),
        ),
      ],
    );
  }
}

class _QuestionCard extends StatelessWidget {
  const _QuestionCard({required this.index, required this.question});

  final int index;
  final BankQuestion question;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final secondaryStyle = Theme.of(context).textTheme.bodySmall?.copyWith(
          color: colorScheme.onSurfaceVariant,
        );

    return Card.filled(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.large),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    '${index + 1}. ${question.stem}',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                if (question.needsReview)
                  Text(
                    '需复核',
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                          color: colorScheme.tertiary,
                        ),
                  ),
              ],
            ),
            if (question.options.isNotEmpty) ...[
              const SizedBox(height: AppSpacing.medium),
              Text(question.options.join('\n')),
            ],
            const SizedBox(height: AppSpacing.medium),
            Text(
              '${bankQuestionTypeLabel(question.questionType)} · '
              '${_difficultyLabel(question.difficulty)}'
              '${question.sourcePage == null ? '' : ' · 第 ${question.sourcePage} 页'}',
              style: secondaryStyle,
            ),
            const SizedBox(height: AppSpacing.small),
            Text(
              '答案：${question.answer?.trim().isNotEmpty == true ? question.answer : '暂无标准答案'}',
              style: secondaryStyle,
            ),
            if (question.analysis?.trim().isNotEmpty == true) ...[
              const SizedBox(height: AppSpacing.small),
              Text('解析：${question.analysis}', style: secondaryStyle),
            ],
          ],
        ),
      ),
    );
  }
}

String _difficultyLabel(BankQuestionDifficulty difficulty) =>
    switch (difficulty) {
      BankQuestionDifficulty.easy => '简单',
      BankQuestionDifficulty.medium => '中等',
      BankQuestionDifficulty.hard => '困难',
      BankQuestionDifficulty.challenge => '挑战',
      BankQuestionDifficulty.unknown => '难度待确认',
    };
