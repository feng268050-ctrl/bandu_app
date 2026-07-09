import 'package:bandu_wrong_notebook/features/library/data/error_item_repository.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemDetailProvider =
    FutureProvider.autoDispose.family<ErrorItemDetail, String>((ref, id) {
  return ref.watch(errorItemRepositoryProvider).fetchErrorItem(id);
});

class ErrorItemDetailPage extends ConsumerWidget {
  const ErrorItemDetailPage({
    required this.errorItemId,
    super.key,
  });

  final String errorItemId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(errorItemDetailProvider(errorItemId));

    return Scaffold(
      appBar: AppBar(title: const Text('错题详情')),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => Center(child: Text(error.toString())),
        data: (item) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(
                item.title,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Text(item.subjectName),
              if (item.questionText != null) ...[
                const SizedBox(height: 24),
                Text(
                  '题目',
                  style: Theme.of(context).textTheme.labelLarge,
                ),
                Text(item.questionText!),
              ],
              if (item.answer != null) ...[
                const SizedBox(height: 24),
                Text(
                  '答案',
                  style: Theme.of(context).textTheme.labelLarge,
                ),
                Text(item.answer!),
              ],
              if (item.analysis != null) ...[
                const SizedBox(height: 24),
                Text(
                  '解析',
                  style: Theme.of(context).textTheme.labelLarge,
                ),
                Text(item.analysis!),
              ],
            ],
          );
        },
      ),
    );
  }
}
