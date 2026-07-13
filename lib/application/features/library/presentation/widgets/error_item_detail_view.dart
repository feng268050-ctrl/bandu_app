import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class ErrorItemDetailView extends StatelessWidget {
  const ErrorItemDetailView({required this.item, super.key});

  final ErrorItemDetail item;

  @override
  Widget build(BuildContext context) {
    return SelectionArea(
      child: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          Text(item.title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: AppSpacing.small),
          Wrap(
            spacing: AppSpacing.small,
            runSpacing: AppSpacing.small,
            children: [
              Chip(label: Text(item.subjectName)),
              Chip(label: Text(masteryLabel(item.masteryLevel))),
            ],
          ),
          if (item.questionText?.trim().isNotEmpty == true)
            _DetailSection(title: '题目', content: item.questionText!),
          if (item.answer?.trim().isNotEmpty == true)
            _DetailSection(title: '答案', content: item.answer!),
          if (item.analysis?.trim().isNotEmpty == true)
            _DetailSection(title: '解析', content: item.analysis!),
        ],
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  const _DetailSection({required this.title, required this.content});

  final String title;
  final String content;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.xLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Divider(),
          const SizedBox(height: AppSpacing.medium),
          Text(title, style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: AppSpacing.small),
          Text(content),
        ],
      ),
    );
  }
}

String masteryLabel(int value) {
  return switch (value) {
    1 => '复习中',
    2 => '已掌握',
    _ => '未掌握',
  };
}
