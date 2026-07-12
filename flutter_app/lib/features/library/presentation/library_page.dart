import 'package:bandu_wrong_notebook/core/widgets/app_async_view.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/features/library/presentation/library_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class LibraryPage extends ConsumerWidget {
  const LibraryPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final items = ref.watch(libraryControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('错题本'),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: () =>
                ref.read(libraryControllerProvider.notifier).refresh(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: AppAsyncView<List<ErrorItemSummary>>(
        value: items,
        data: (data) {
          if (data.isEmpty) {
            return const Center(child: Text('暂无错题'));
          }

          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: data.length,
            separatorBuilder: (context, index) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final item = data[index];
              return ListTile(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                tileColor: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest,
                title: Text(item.title),
                subtitle: Text(item.subjectName),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => context.go('/library/${item.id}'),
              );
            },
          );
        },
      ),
    );
  }
}
