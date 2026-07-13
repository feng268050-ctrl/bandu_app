import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class PracticeQuestionCard extends StatelessWidget {
  const PracticeQuestionCard({
    required this.question,
    required this.showAnswer,
    required this.onToggleAnswer,
    super.key,
  });

  final PracticeQuestion question;
  final bool showAnswer;
  final VoidCallback onToggleAnswer;

  @override
  Widget build(BuildContext context) {
    return Card.filled(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.large),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              question.title,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: AppSpacing.small),
            Text(question.questionText),
            if (question.tags.isNotEmpty) ...[
              const SizedBox(height: AppSpacing.medium),
              Wrap(
                spacing: AppSpacing.small,
                runSpacing: AppSpacing.small,
                children: [
                  for (final tag in question.tags) Chip(label: Text(tag)),
                ],
              ),
            ],
            const SizedBox(height: AppSpacing.medium),
            AppDefaultButton(
              label: showAnswer ? '隐藏答案' : '查看答案',
              icon: Icon(
                showAnswer
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
              ),
              onPressed: onToggleAnswer,
            ),
            if (showAnswer) ...[
              const SizedBox(height: AppSpacing.medium),
              Text('答案', style: Theme.of(context).textTheme.labelLarge),
              Text(question.answer),
              const SizedBox(height: AppSpacing.medium),
              Text('解析', style: Theme.of(context).textTheme.labelLarge),
              Text(question.analysis),
            ],
          ],
        ),
      ),
    );
  }
}
