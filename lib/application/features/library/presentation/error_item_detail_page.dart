import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/widgets/error_item_detail_view.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/widgets/error_item_edit_sheet.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final errorItemDetailProvider =
    FutureProvider.autoDispose.family<ErrorItemDetail, String>((ref, id) {
  return ref.watch(fetchErrorItemDetailUseCaseProvider).call(id);
});

class ErrorItemDetailPage extends ConsumerWidget {
  const ErrorItemDetailPage({required this.errorItemId, super.key});

  final String errorItemId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(errorItemDetailProvider(errorItemId));

    return Scaffold(
      appBar: AppBar(
        title: const Text('错题详情'),
        actions: [
          IconButton(
            tooltip: '编辑',
            onPressed: detail.valueOrNull == null
                ? null
                : () => _editItem(context, ref, detail.requireValue),
            icon: const Icon(Icons.edit_outlined),
          ),
          IconButton(
            tooltip: '删除',
            color: Theme.of(context).colorScheme.error,
            onPressed: detail.valueOrNull == null
                ? null
                : () => _deleteItem(context, ref, detail.requireValue),
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
      body: detail.when(
        loading: () => const AppLoadingView(message: '正在加载错题详情'),
        error: (error, stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(errorItemDetailProvider(errorItemId)),
        ),
        data: (item) => ErrorItemDetailView(item: item),
      ),
    );
  }

  Future<void> _editItem(
    BuildContext context,
    WidgetRef ref,
    ErrorItemDetail item,
  ) async {
    final saved = await showErrorItemEditSheet(
      context: context,
      item: item,
      onSave: (update) async {
        await ref.read(updateErrorItemUseCaseProvider).call(item.id, update);
        ref.invalidate(errorItemDetailProvider(item.id));
        ref.invalidate(libraryControllerProvider);
        ref.invalidate(statsOverviewProvider);
      },
    );
    if (saved && context.mounted) {
      showAppSuccessSnackBar(context, '错题已更新');
    }
  }

  Future<void> _deleteItem(
    BuildContext context,
    WidgetRef ref,
    ErrorItemDetail item,
  ) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除错题',
      message: '删除后无法恢复，确定继续吗？',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed || !context.mounted) {
      return;
    }

    try {
      await ref.read(deleteErrorItemUseCaseProvider).call(item.id);
      ref.invalidate(libraryControllerProvider);
      ref.invalidate(statsOverviewProvider);
      if (context.mounted) {
        showAppSuccessSnackBar(context, '错题已删除');
        context.go('/home/library');
      }
    } catch (error) {
      if (context.mounted) {
        showAppErrorSnackBar(context, '删除失败：$error');
      }
    }
  }
}
