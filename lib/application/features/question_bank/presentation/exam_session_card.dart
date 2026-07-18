import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_bank_labels.dart';
import 'package:flutter/material.dart';

class ExamSessionCard extends StatelessWidget {
  const ExamSessionCard({
    required this.session,
    required this.onOpen,
    this.trailing,
    super.key,
  });

  final ExamSession session;
  final VoidCallback onOpen;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final completed = session.status == ExamSessionStatus.completed;
    final progress = completed
        ? '已完成 · 正确 ${session.correctCount} 题 · '
            '错误 ${session.incorrectCount} 题'
        : '继续作答 · 已提交 ${session.answeredCount} / '
            '${session.attempts.length} 题';
    return Card.outlined(
      child: ListTile(
        leading: Icon(
          completed ? Icons.task_alt : Icons.assignment_outlined,
        ),
        title: Text(session.title),
        subtitle: Text(
          '${bankQuestionTypeSummary(session.questionTypeCounts)}\n'
          '$progress · ${_date(session.createdAt)}',
        ),
        isThreeLine: true,
        trailing: trailing ?? const Icon(Icons.chevron_right),
        onTap: onOpen,
      ),
    );
  }

  String _date(DateTime value) {
    final local = value.toLocal();
    return '${local.month.toString().padLeft(2, '0')}-'
        '${local.day.toString().padLeft(2, '0')} '
        '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')}';
  }
}
