import 'package:bandu_wrong_notebook/application/features/stats/presentation/widgets/stats_tile.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class StatsPage extends ConsumerStatefulWidget {
  const StatsPage({
    required this.initialPeriod,
    this.highlightedMetric,
    super.key,
  });

  final StatsPeriod initialPeriod;
  final String? highlightedMetric;

  @override
  ConsumerState<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends ConsumerState<StatsPage> {
  late StatsPeriod _period;

  @override
  void initState() {
    super.initState();
    _period = widget.initialPeriod;
  }

  @override
  Widget build(BuildContext context) {
    final stats = ref.watch(statsOverviewProvider(_period));

    return Scaffold(
      appBar: AppBar(
        title: Text('${_period == StatsPeriod.week ? '本周' : '本月'}详情'),
        actions: [
          PopupMenuButton<StatsPeriod>(
            tooltip: '选择统计周期',
            initialValue: _period,
            onSelected: (period) => setState(() => _period = period),
            itemBuilder: (context) => const [
              PopupMenuItem(value: StatsPeriod.week, child: Text('本周')),
              PopupMenuItem(value: StatsPeriod.month, child: Text('本月')),
            ],
            icon: const Icon(Icons.date_range_outlined),
          ),
        ],
      ),
      body: stats.when(
        loading: () => const AppLoadingView(message: '正在读取学习统计'),
        error: (error, stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(statsOverviewProvider(_period)),
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
                    isHighlighted: widget.highlightedMetric == 'errors',
                  ),
                1 => StatsTile(
                    label: '已掌握',
                    value: data.masteredCount.toString(),
                    icon: Icons.task_alt_outlined,
                    isHighlighted: widget.highlightedMetric == 'mastered',
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
                    isHighlighted: widget.highlightedMetric == 'accuracy',
                  ),
              };
            },
          );
        },
      ),
    );
  }
}
