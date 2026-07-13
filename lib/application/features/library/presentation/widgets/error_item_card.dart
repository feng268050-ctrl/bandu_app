import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:flutter/material.dart';

class ErrorItemCard extends StatelessWidget {
  const ErrorItemCard({required this.item, required this.onTap, super.key});

  final ErrorItemSummary item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card.outlined(
      child: ListTile(
        leading: Icon(
          item.mastered ? Icons.check_circle_outline : Icons.menu_book_outlined,
        ),
        title: Text(item.title, maxLines: 2, overflow: TextOverflow.ellipsis),
        subtitle: Text(item.subjectName),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
