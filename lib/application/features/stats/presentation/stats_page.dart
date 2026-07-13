import 'package:bandu_wrong_notebook/application/features/stats/presentation/widgets/stats_tile.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class StatsPage extends ConsumerWidget {
  const StatsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final stats = ref.watch(statsOverviewProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('统计')),
      body: stats.when(
        loading: () => const AppLoadingView(message: '正在读取学习统计'),
        error: (error, stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(statsOverviewProvider),
        ),
        data: (data) {
          return ListView.separated(
            padding: const EdgeInsets.all(AppSpacing.page),
            itemCount: 5,
            separatorBuilder: (context, index) =>
                const SizedBox(height: AppSpacing.small),
            itemBuilder: (context, index) {
              return switch (index) {
                0 => StatsTile(
                    label: '错题总数',
                    value: data.totalErrors.toString(),
                    icon: Icons.library_books_outlined,
                  ),
                1 => StatsTile(
                    label: '已掌握',
                    value: data.masteredCount.toString(),
                    icon: Icons.task_alt_outlined,
                  ),
                2 => StatsTile(
                    label: '掌握率',
                    value: '${(data.masteryRate * 100).toStringAsFixed(0)}%',
                    icon: Icons.donut_large_outlined,
                  ),
                3 => StatsTile(
                    label: '练习次数',
                    value: data.practiceTotal.toString(),
                    icon: Icons.quiz_outlined,
                  ),
                _ => StatsTile(
                    label: '练习正确率',
                    value:
                        '${(data.practiceAccuracy * 100).toStringAsFixed(0)}%',
                    icon: Icons.trending_up,
                  ),
              };
            },
          );
        },
      ),
    );
  }
}
