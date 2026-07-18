import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_session_controller.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_labels.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class ExamSessionPage extends ConsumerStatefulWidget {
  const ExamSessionPage({required this.sessionId, super.key});

  final String sessionId;

  @override
  ConsumerState<ExamSessionPage> createState() => _ExamSessionPageState();
}

class _ExamSessionPageState extends ConsumerState<ExamSessionPage> {
  @override
  void initState() {
    super.initState();
    Future<void>.microtask(
      () => ref
          .read(examSessionControllerProvider.notifier)
          .load(widget.sessionId),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(examSessionControllerProvider);
    final controller = ref.read(examSessionControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('随机考卷')),
      body: switch (state.phase) {
        ExamSessionPhase.idle ||
        ExamSessionPhase.loading =>
          const AppLoadingView(message: '正在加载考卷'),
        ExamSessionPhase.failed => AppErrorView(
            message: state.errorMessage ?? '考卷加载失败',
            onRetry: () => controller.load(widget.sessionId),
          ),
        ExamSessionPhase.ready || ExamSessionPhase.submitting => _ExamContent(
            state: state,
            onAnswerChanged: controller.updateAnswer,
            onSubmit: controller.submitCurrentAnswer,
            onPrevious: controller.previousAttempt,
            onNext: controller.nextAttempt,
            onReturnToBank: () => context.go('/capture/pdf-import'),
          ),
      },
    );
  }
}

class _ExamContent extends StatelessWidget {
  const _ExamContent({
    required this.state,
    required this.onAnswerChanged,
    required this.onSubmit,
    required this.onPrevious,
    required this.onNext,
    required this.onReturnToBank,
  });

  final ExamSessionUiState state;
  final ValueChanged<String> onAnswerChanged;
  final VoidCallback onSubmit;
  final VoidCallback onPrevious;
  final VoidCallback onNext;
  final VoidCallback onReturnToBank;

  @override
  Widget build(BuildContext context) {
    final session = state.session!;
    final attempt = state.currentAttempt!;
    final isFirst = state.currentAttemptIndex == 0;
    final isLast = state.currentAttemptIndex == session.attempts.length - 1;

    return ListView(
      padding: const EdgeInsets.all(AppSpacing.page),
      children: [
        _ExamProgress(
          session: session,
          currentAttemptIndex: state.currentAttemptIndex,
        ),
        if (session.status == ExamSessionStatus.completed) ...[
          const SizedBox(height: AppSpacing.medium),
          _ExamSummary(session: session),
        ],
        const SizedBox(height: AppSpacing.medium),
        _QuestionCard(
          attempt: attempt,
          answerInput: state.answerInput,
          isSubmitting: state.phase == ExamSessionPhase.submitting,
          errorMessage: state.errorMessage,
          onAnswerChanged: onAnswerChanged,
          onSubmit: onSubmit,
        ),
        if (attempt.answerRevealed) ...[
          const SizedBox(height: AppSpacing.medium),
          Row(
            children: [
              Expanded(
                child: AppDefaultButton(
                  label: '上一题',
                  icon: const Icon(Icons.arrow_back),
                  onPressed: isFirst ? null : onPrevious,
                ),
              ),
              const SizedBox(width: AppSpacing.medium),
              Expanded(
                child: isLast
                    ? AppPrimaryButton(
                        label: '返回题库',
                        icon: const Icon(Icons.check),
                        onPressed: onReturnToBank,
                      )
                    : AppPrimaryButton(
                        label: '下一题',
                        icon: const Icon(Icons.arrow_forward),
                        onPressed: onNext,
                      ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

class _ExamProgress extends StatelessWidget {
  const _ExamProgress({
    required this.session,
    required this.currentAttemptIndex,
  });

  final ExamSession session;
  final int currentAttemptIndex;

  @override
  Widget build(BuildContext context) {
    return Card.filled(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.large),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(session.title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: AppSpacing.small),
            Text(
              bankQuestionTypeSummary(session.questionTypeCounts),
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
            ),
            const SizedBox(height: AppSpacing.small),
            Text(
              '第 ${currentAttemptIndex + 1} / ${session.attempts.length} 题'
              ' · 已提交 ${session.answeredCount} 题',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
            ),
            const SizedBox(height: AppSpacing.small),
            LinearProgressIndicator(
              value: session.answeredCount / session.attempts.length,
            ),
          ],
        ),
      ),
    );
  }
}

class _ExamSummary extends StatelessWidget {
  const _ExamSummary({required this.session});

  final ExamSession session;

  @override
  Widget build(BuildContext context) {
    return Card.outlined(
      child: ListTile(
        leading: const Icon(Icons.fact_check_outlined),
        title: const Text('考卷已完成'),
        subtitle: Text(
          '正确 ${session.correctCount} 题 · 错误 ${session.incorrectCount} 题'
          ' · 待复核 ${session.reviewCount} 题',
        ),
      ),
    );
  }
}

class _QuestionCard extends StatelessWidget {
  const _QuestionCard({
    required this.attempt,
    required this.answerInput,
    required this.isSubmitting,
    required this.errorMessage,
    required this.onAnswerChanged,
    required this.onSubmit,
  });

  final ExamAttempt attempt;
  final String answerInput;
  final bool isSubmitting;
  final String? errorMessage;
  final ValueChanged<String> onAnswerChanged;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final question = attempt.question;
    final colorScheme = Theme.of(context).colorScheme;

    return Card.filled(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.large),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(question.stem, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: AppSpacing.medium),
            if (!attempt.answerRevealed)
              _AnswerEditor(
                key: ValueKey(attempt.id),
                question: question,
                answer: answerInput,
                enabled: !isSubmitting,
                onChanged: onAnswerChanged,
              )
            else
              _AnswerResult(attempt: attempt),
            if (errorMessage != null) ...[
              const SizedBox(height: AppSpacing.small),
              Text(
                errorMessage!,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: colorScheme.error,
                    ),
              ),
            ],
            if (!attempt.answerRevealed) ...[
              const SizedBox(height: AppSpacing.medium),
              AppAsyncPrimaryButton(
                label: '提交并查看答案',
                expanded: true,
                isLoading: isSubmitting,
                icon: const Icon(Icons.check),
                onPressed: isSubmitting ? null : onSubmit,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _AnswerEditor extends StatelessWidget {
  const _AnswerEditor({
    required this.question,
    required this.answer,
    required this.enabled,
    required this.onChanged,
    super.key,
  });

  final BankQuestion question;
  final String answer;
  final bool enabled;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return switch (question.questionType) {
      BankQuestionType.singleChoice when question.options.isNotEmpty =>
        _ChoiceAnswer(
          options: question.options,
          answer: answer,
          multiple: false,
          enabled: enabled,
          onChanged: onChanged,
        ),
      BankQuestionType.multipleChoice when question.options.isNotEmpty =>
        _ChoiceAnswer(
          options: question.options,
          answer: answer,
          multiple: true,
          enabled: enabled,
          onChanged: onChanged,
        ),
      _ => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (question.options.isNotEmpty) ...[
              Text(question.options.join('\n')),
              const SizedBox(height: AppSpacing.medium),
            ],
            TextFormField(
              initialValue: answer,
              enabled: enabled,
              minLines:
                  question.questionType == BankQuestionType.subjective ? 3 : 1,
              maxLines:
                  question.questionType == BankQuestionType.subjective ? 6 : 3,
              textInputAction:
                  question.questionType == BankQuestionType.subjective
                      ? TextInputAction.newline
                      : TextInputAction.done,
              decoration: const InputDecoration(
                labelText: '你的答案',
                prefixIcon: Icon(Icons.edit_outlined),
              ),
              onChanged: onChanged,
            ),
          ],
        ),
    };
  }
}

class _ChoiceAnswer extends StatelessWidget {
  const _ChoiceAnswer({
    required this.options,
    required this.answer,
    required this.multiple,
    required this.enabled,
    required this.onChanged,
  });

  final List<String> options;
  final String answer;
  final bool multiple;
  final bool enabled;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final selected = answer
        .toUpperCase()
        .split(RegExp(r'[,，、;；\s]+'))
        .where((item) => item.isNotEmpty)
        .toSet();

    if (!multiple) {
      return RadioGroup<String>(
        groupValue: selected.firstOrNull,
        onChanged: (value) {
          if (enabled && value != null) onChanged(value);
        },
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            for (var index = 0; index < options.length; index++)
              RadioListTile<String>(
                contentPadding: EdgeInsets.zero,
                value: _optionKey(options[index], index),
                title: Text(options[index]),
                enabled: enabled,
              ),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var index = 0; index < options.length; index++)
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            controlAffinity: ListTileControlAffinity.leading,
            value: selected.contains(_optionKey(options[index], index)),
            title: Text(options[index]),
            onChanged: enabled
                ? (checked) {
                    final updated = {...selected};
                    final key = _optionKey(options[index], index);
                    checked == true ? updated.add(key) : updated.remove(key);
                    final ordered = updated.toList()..sort();
                    onChanged(ordered.join(','));
                  }
                : null,
          ),
      ],
    );
  }
}

class _AnswerResult extends StatelessWidget {
  const _AnswerResult({required this.attempt});

  final ExamAttempt attempt;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final resultColor = switch (attempt.gradingResult) {
      ExamGradingResult.correct => colorScheme.primary,
      ExamGradingResult.incorrect => colorScheme.error,
      ExamGradingResult.needsReview => colorScheme.tertiary,
      ExamGradingResult.notGraded => colorScheme.onSurfaceVariant,
    };
    final resultIcon = switch (attempt.gradingResult) {
      ExamGradingResult.correct => Icons.check_circle_outline,
      ExamGradingResult.incorrect => Icons.cancel_outlined,
      ExamGradingResult.needsReview => Icons.pending_actions_outlined,
      ExamGradingResult.notGraded => Icons.info_outline,
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(resultIcon, color: resultColor),
            const SizedBox(width: AppSpacing.small),
            Text(
              _gradingLabel(attempt.gradingResult),
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: resultColor,
                  ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.medium),
        Text('你的答案', style: Theme.of(context).textTheme.labelLarge),
        Text(attempt.userAnswer ?? ''),
        const SizedBox(height: AppSpacing.medium),
        Text('标准答案', style: Theme.of(context).textTheme.labelLarge),
        Text(attempt.question.answer?.trim().isNotEmpty == true
            ? attempt.question.answer!
            : '暂无标准答案'),
        if (attempt.question.analysis?.trim().isNotEmpty == true) ...[
          const SizedBox(height: AppSpacing.medium),
          Text('解析', style: Theme.of(context).textTheme.labelLarge),
          Text(attempt.question.analysis!),
        ],
        if (attempt.gradingFeedback != null) ...[
          const SizedBox(height: AppSpacing.medium),
          Text(
            attempt.gradingFeedback!,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
          ),
        ],
      ],
    );
  }
}

String _optionKey(String option, int index) {
  final match = RegExp(r'^\s*([A-Za-z])(?:[.、:：)）\s]|$)').firstMatch(option);
  return match?.group(1)?.toUpperCase() ??
      String.fromCharCode('A'.codeUnitAt(0) + index);
}

String _gradingLabel(ExamGradingResult result) => switch (result) {
      ExamGradingResult.correct => '回答正确',
      ExamGradingResult.incorrect => '回答错误',
      ExamGradingResult.needsReview => '等待复核',
      ExamGradingResult.notGraded => '暂未批改',
    };
