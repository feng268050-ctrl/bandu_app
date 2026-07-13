import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/widgets/error_item_card.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
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
      body: items.when(
        loading: () => const AppLoadingView(message: '正在加载错题'),
        error: (error, stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.read(libraryControllerProvider.notifier).refresh(),
        ),
        data: (data) {
          if (data.isEmpty) {
            return AppEmptyView(
              title: '暂无错题',
              message: '拍摄题目并确认分析结果后，会保存在这里。',
              icon: Icons.library_books_outlined,
              actionLabel: '去拍题',
              onAction: () => context.go('/capture'),
            );
          }

          return ListView.separated(
            padding: const EdgeInsets.all(AppSpacing.page),
            itemCount: data.length,
            separatorBuilder: (context, index) =>
                const SizedBox(height: AppSpacing.small),
            itemBuilder: (context, index) {
              final item = data[index];
              return ErrorItemCard(
                item: item,
                onTap: () => context.go('/library/${item.id}'),
              );
            },
          );
        },
      ),
    );
  }
}
