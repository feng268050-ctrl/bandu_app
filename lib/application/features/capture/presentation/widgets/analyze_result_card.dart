import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class AnalyzeResultCard extends StatelessWidget {
  const AnalyzeResultCard({required this.result, super.key});

  final AnalyzeResult result;

  @override
  Widget build(BuildContext context) {
    return Card.filled(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.large),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(result.title, style: Theme.of(context).textTheme.titleMedium),
            if (result.questionText case final questionText?) ...[
              const SizedBox(height: AppSpacing.medium),
              Text(questionText),
            ],
            if (result.answer case final answer?) ...[
              const SizedBox(height: AppSpacing.medium),
              Text('答案', style: Theme.of(context).textTheme.labelLarge),
              Text(answer),
            ],
            if (result.analysis case final analysis?) ...[
              const SizedBox(height: AppSpacing.medium),
              Text('解析', style: Theme.of(context).textTheme.labelLarge),
              Text(analysis),
            ],
          ],
        ),
      ),
    );
  }
}
