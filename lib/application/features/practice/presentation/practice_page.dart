import 'package:bandu_wrong_notebook/application/features/practice/practice_providers.dart';
import 'package:bandu_wrong_notebook/application/features/practice/presentation/practice_controller.dart';
import 'package:bandu_wrong_notebook/application/features/practice/presentation/widgets/practice_history_list.dart';
import 'package:bandu_wrong_notebook/application/features/practice/presentation/widgets/practice_question_card.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class PracticePage extends ConsumerWidget {
  const PracticePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(practiceControllerProvider);
    final controller = ref.read(practiceControllerProvider.notifier);
    final question = state.question;
    final history = ref.watch(practiceHistoryProvider);
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('练习')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          AppAsyncPrimaryButton(
            label: '从最新错题生成练习',
            expanded: true,
            isLoading: state.isBusy && question == null,
            onPressed: state.isBusy ? null : controller.generateFromLatestError,
            icon: const Icon(Icons.play_arrow),
          ),
          if (state.errorMessage != null) ...[
            const SizedBox(height: AppSpacing.medium),
            Text(
              state.errorMessage!,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: colorScheme.error),
            ),
          ],
          if (state.noticeMessage != null) ...[
            const SizedBox(height: AppSpacing.medium),
            Text(
              state.noticeMessage!,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: colorScheme.primary),
            ),
          ],
          if (question == null) ...[
            const SizedBox(height: AppSpacing.large),
            const Card.filled(
              child: ListTile(
                leading: Icon(Icons.quiz_outlined),
                title: Text('尚未生成练习'),
                subtitle: Text('生成后可查看答案并记录答题结果。'),
              ),
            ),
          ] else ...[
            const SizedBox(height: AppSpacing.large),
            if (question.resolvedModel != null)
              Card.outlined(
                child: ListTile(
                  leading: const Icon(Icons.auto_awesome_outlined),
                  title: Text(question.resolvedModel!.fallbackOccurred
                      ? '主模型不可用，已切换到备用模型'
                      : '由 Auto 选择：${question.resolvedModel!.displayName}'),
                  subtitle: question.resolvedModel!.fallbackOccurred
                      ? Text(question.resolvedModel!.displayName)
                      : null,
                ),
              ),
            PracticeQuestionCard(
              question: question,
              showAnswer: state.showAnswer,
              onToggleAnswer: controller.toggleAnswer,
            ),
            const SizedBox(height: AppSpacing.medium),
            Row(
              children: [
                Expanded(
                  child: AppDefaultButton(
                    label: '答错',
                    onPressed: state.isBusy
                        ? null
                        : () => controller.recordResult(false),
                    icon: const Icon(Icons.close),
                  ),
                ),
                const SizedBox(width: AppSpacing.medium),
                Expanded(
                  child: AppPrimaryButton(
                    label: '答对',
                    onPressed: state.isBusy
                        ? null
                        : () => controller.recordResult(true),
                    icon: const Icon(Icons.check),
                  ),
                ),
              ],
            ),
          ],
          const SizedBox(height: AppSpacing.xLarge),
          Text('最近练习', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: AppSpacing.small),
          history.when(
            loading: () => const LinearProgressIndicator(),
            error: (error, stackTrace) => ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Icon(Icons.error_outline, color: colorScheme.error),
              title: const Text('练习记录加载失败'),
              trailing: IconButton(
                tooltip: '重试',
                onPressed: () => ref.invalidate(practiceHistoryProvider),
                icon: const Icon(Icons.refresh),
              ),
            ),
            data: (records) {
              if (records.isEmpty) {
                return const AppEmptyView(
                  title: '暂无练习记录',
                  message: '完成一道练习后会显示在这里。',
                  icon: Icons.history,
                );
              }
              return PracticeHistoryList(records: records);
            },
          ),
        ],
      ),
    );
  }
}
