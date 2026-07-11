import 'package:bandu_wrong_notebook/features/practice/presentation/practice_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class PracticePage extends ConsumerWidget {
  const PracticePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(practiceControllerProvider);
    final controller = ref.read(practiceControllerProvider.notifier);
    final question = state.question;

    return Scaffold(
      appBar: AppBar(title: const Text('练习')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          FilledButton.icon(
            onPressed: state.isBusy ? null : controller.generateFromLatestError,
            icon: state.isBusy
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.play_arrow),
            label: const Text('从最新错题生成练习'),
          ),
          if (state.errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(
              state.errorMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          if (state.noticeMessage != null) ...[
            const SizedBox(height: 12),
            Text(
              state.noticeMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.primary),
            ),
          ],
          if (question == null) ...[
            const SizedBox(height: 16),
            const Card(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Text('生成练习后，可查看答案并记录答题结果。'),
              ),
            ),
          ] else ...[
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      question.title,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(question.questionText),
                    if (question.tags.isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          for (final tag in question.tags)
                            Chip(label: Text(tag)),
                        ],
                      ),
                    ],
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: controller.toggleAnswer,
                      icon: Icon(
                        state.showAnswer
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                      ),
                      label: Text(state.showAnswer ? '隐藏答案' : '查看答案'),
                    ),
                    if (state.showAnswer) ...[
                      const SizedBox(height: 12),
                      Text(
                        '答案',
                        style: Theme.of(context).textTheme.labelLarge,
                      ),
                      Text(question.answer),
                      const SizedBox(height: 12),
                      Text(
                        '解析',
                        style: Theme.of(context).textTheme.labelLarge,
                      ),
                      Text(question.analysis),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed:
                        state.isBusy ? null : () => controller.recordResult(false),
                    icon: const Icon(Icons.close),
                    label: const Text('答错'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton.icon(
                    onPressed:
                        state.isBusy ? null : () => controller.recordResult(true),
                    icon: const Icon(Icons.check),
                    label: const Text('答对'),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
