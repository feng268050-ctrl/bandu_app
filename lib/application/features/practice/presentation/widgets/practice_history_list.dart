import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:flutter/material.dart';

class PracticeHistoryList extends StatelessWidget {
  const PracticeHistoryList({required this.records, super.key});

  final List<PracticeRecord> records;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (var index = 0; index < records.length; index++) ...[
          _PracticeHistoryTile(record: records[index]),
          if (index != records.length - 1) const Divider(),
        ],
      ],
    );
  }
}

class _PracticeHistoryTile extends StatelessWidget {
  const _PracticeHistoryTile({required this.record});

  final PracticeRecord record;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(
        record.isCorrect ? Icons.check_circle : Icons.cancel,
        color: record.isCorrect ? colorScheme.primary : colorScheme.error,
      ),
      title: Text(record.subject),
      subtitle: Text(_difficultyLabel(record.difficulty)),
      trailing: Text(record.isCorrect ? '答对' : '答错'),
    );
  }
}

String _difficultyLabel(String value) {
  return switch (value) {
    'easy' => '简单',
    'hard' => '困难',
    'harder' => '挑战',
    _ => '中等',
  };
}
