import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/features/stats/data/stats_api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final errorItemDetailProvider = FutureProvider.autoDispose
    .family<ErrorItemDetail, String>((ref, id) {
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
            onPressed: detail.valueOrNull == null
                ? null
                : () => _deleteItem(context, ref, detail.requireValue),
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => _ErrorState(
          message: error.toString(),
          onRetry: () => ref.invalidate(errorItemDetailProvider(errorItemId)),
        ),
        data: (item) => _DetailBody(item: item),
      ),
    );
  }

  Future<void> _editItem(
    BuildContext context,
    WidgetRef ref,
    ErrorItemDetail item,
  ) async {
    final questionController = TextEditingController(text: item.questionText);
    final answerController = TextEditingController(text: item.answer);
    final analysisController = TextEditingController(text: item.analysis);
    var masteryLevel = item.masteryLevel;
    var saving = false;
    String? errorMessage;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return Padding(
              padding: EdgeInsets.fromLTRB(
                20,
                0,
                20,
                MediaQuery.viewInsetsOf(context).bottom + 20,
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('编辑错题', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 16),
                    TextField(
                      controller: questionController,
                      minLines: 3,
                      maxLines: 8,
                      decoration: const InputDecoration(
                        labelText: '题目',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: answerController,
                      minLines: 2,
                      maxLines: 6,
                      decoration: const InputDecoration(
                        labelText: '答案',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: analysisController,
                      minLines: 3,
                      maxLines: 8,
                      decoration: const InputDecoration(
                        labelText: '解析',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<int>(
                      initialValue: masteryLevel,
                      decoration: const InputDecoration(
                        labelText: '掌握状态',
                        border: OutlineInputBorder(),
                      ),
                      items: const [
                        DropdownMenuItem(value: 0, child: Text('未掌握')),
                        DropdownMenuItem(value: 1, child: Text('复习中')),
                        DropdownMenuItem(value: 2, child: Text('已掌握')),
                      ],
                      onChanged: saving
                          ? null
                          : (value) => setModalState(
                              () => masteryLevel = value ?? masteryLevel,
                            ),
                    ),
                    if (errorMessage != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        errorMessage!,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: saving
                          ? null
                          : () async {
                              if (questionController.text.trim().isEmpty) {
                                setModalState(() => errorMessage = '题目不能为空');
                                return;
                              }
                              setModalState(() {
                                saving = true;
                                errorMessage = null;
                              });
                              try {
                                await ref
                                    .read(updateErrorItemUseCaseProvider)
                                    .call(
                                      item.id,
                                      ErrorItemUpdate(
                                        questionText: questionController.text,
                                        answer: answerController.text,
                                        analysis: analysisController.text,
                                        masteryLevel: masteryLevel,
                                      ),
                                    );
                                ref.invalidate(
                                  errorItemDetailProvider(item.id),
                                );
                                ref.invalidate(libraryControllerProvider);
                                ref.invalidate(statsOverviewProvider);
                                if (sheetContext.mounted) {
                                  Navigator.of(sheetContext).pop();
                                }
                              } catch (error) {
                                setModalState(() {
                                  saving = false;
                                  errorMessage = error.toString();
                                });
                              }
                            },
                      icon: saving
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.save_outlined),
                      label: const Text('保存修改'),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );

    questionController.dispose();
    answerController.dispose();
    analysisController.dispose();
  }

  Future<void> _deleteItem(
    BuildContext context,
    WidgetRef ref,
    ErrorItemDetail item,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除错题'),
        content: const Text('删除后无法恢复，确定继续吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) {
      return;
    }

    try {
      await ref.read(deleteErrorItemUseCaseProvider).call(item.id);
      ref.invalidate(libraryControllerProvider);
      ref.invalidate(statsOverviewProvider);
      if (context.mounted) {
        context.go('/library');
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('删除失败：$error')));
      }
    }
  }
}

class _DetailBody extends StatelessWidget {
  const _DetailBody({required this.item});

  final ErrorItemDetail item;

  @override
  Widget build(BuildContext context) {
    return SelectionArea(
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(item.title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              Chip(label: Text(item.subjectName)),
              Chip(label: Text(_masteryLabel(item.masteryLevel))),
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
      padding: const EdgeInsets.only(top: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Divider(),
          const SizedBox(height: 12),
          Text(title, style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 8),
          Text(content),
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }
}

String _masteryLabel(int value) {
  return switch (value) {
    1 => '复习中',
    2 => '已掌握',
    _ => '未掌握',
  };
}
