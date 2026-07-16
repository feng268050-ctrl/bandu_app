import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_metric.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/application/features/stats/presentation/widgets/stats_tile.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class StatsPage extends ConsumerStatefulWidget {
  const StatsPage({
    required this.initialPeriod,
    required this.metric,
    super.key,
  });

  final StatsPeriod initialPeriod;
  final StatsMetric metric;

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
        title: Text('${_period.label}${_metricTitle(widget.metric)}详情'),
        actions: [
          PopupMenuButton<StatsPeriod>(
            tooltip: '选择统计周期',
            initialValue: _period,
            onSelected: (period) => setState(() => _period = period),
            itemBuilder: (context) => StatsPeriod.values
                .map(
                  (period) => PopupMenuItem(
                    value: period,
                    child: Text(period.label),
                  ),
                )
                .toList(),
            icon: const Icon(Icons.date_range_outlined),
          ),
        ],
      ),
      body: stats.when(
        loading: () => const AppLoadingView(message: '正在读取学习统计'),
        error: (error, stackTrace) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: () => ref.invalidate(statsOverviewProvider(_period)),
        ),
        data: (data) => ListView(
          padding: const EdgeInsets.all(AppSpacing.page),
          children: [
            if (data.isFromCache) ...[
              const Chip(
                avatar: Icon(Icons.offline_bolt_outlined, size: 18),
                label: Text('正在显示离线缓存'),
              ),
              const SizedBox(height: AppSpacing.medium),
            ],
            _MetricSummary(
              metric: widget.metric,
              period: _period,
              data: data,
            ),
            const SizedBox(height: AppSpacing.xLarge),
            Text('相关数据', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppSpacing.medium),
            ..._relatedTiles(widget.metric, data),
            if (widget.metric != StatsMetric.accuracy) ...[
              const SizedBox(height: AppSpacing.large),
              AppDefaultButton(
                label: '查看错题本',
                expanded: true,
                icon: const Icon(Icons.library_books_outlined),
                onPressed: () => context.go('/home/library'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  List<Widget> _relatedTiles(StatsMetric metric, StatsOverview data) {
    final gap = const SizedBox(height: AppSpacing.small);
    return switch (metric) {
      StatsMetric.errors => [
          StatsTile(
            label: '已掌握',
            value: data.masteredCount.toString(),
            icon: Icons.task_alt_outlined,
          ),
          gap,
          StatsTile(
            label: '掌握率',
            value: '${(data.masteryRate * 100).toStringAsFixed(0)}%',
            icon: Icons.donut_large_outlined,
          ),
          gap,
          StatsTile(
            label: '练习次数',
            value: data.practiceTotal.toString(),
            icon: Icons.quiz_outlined,
          ),
        ],
      StatsMetric.mastered => [
          StatsTile(
            label: '错题总数',
            value: data.totalErrors.toString(),
            icon: Icons.library_books_outlined,
          ),
          gap,
          StatsTile(
            label: '未掌握',
            value: (data.totalErrors - data.masteredCount)
                .clamp(0, 1 << 31)
                .toString(),
            icon: Icons.pending_actions_outlined,
          ),
          gap,
          StatsTile(
            label: '掌握率',
            value: '${(data.masteryRate * 100).toStringAsFixed(0)}%',
            icon: Icons.donut_large_outlined,
          ),
        ],
      StatsMetric.accuracy => [
          StatsTile(
            label: '练习次数',
            value: data.practiceTotal.toString(),
            icon: Icons.quiz_outlined,
          ),
          gap,
          StatsTile(
            label: '答对次数',
            value: data.practiceCorrect.toString(),
            icon: Icons.check_circle_outline,
          ),
          gap,
          StatsTile(
            label: '答错次数',
            value: (data.practiceTotal - data.practiceCorrect)
                .clamp(0, 1 << 31)
                .toString(),
            icon: Icons.cancel_outlined,
          ),
        ],
    };
  }
}

class _MetricSummary extends StatelessWidget {
  const _MetricSummary({
    required this.metric,
    required this.period,
    required this.data,
  });

  final StatsMetric metric;
  final StatsPeriod period;
  final StatsOverview data;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final value = switch (metric) {
      StatsMetric.errors => data.totalErrors.toString(),
      StatsMetric.mastered => data.masteredCount.toString(),
      StatsMetric.accuracy =>
        '${(data.practiceAccuracy * 100).toStringAsFixed(0)}%',
    };
    final icon = switch (metric) {
      StatsMetric.errors => Icons.library_books_outlined,
      StatsMetric.mastered => Icons.task_alt_outlined,
      StatsMetric.accuracy => Icons.trending_up,
    };
    final description = switch (metric) {
      StatsMetric.errors => '${period.label}新收录的错题数量',
      StatsMetric.mastered => '${period.label}收录错题中当前已掌握的数量',
      StatsMetric.accuracy => data.practiceTotal == 0
          ? '${period.label}暂无练习记录'
          : '${period.label}共练习 ${data.practiceTotal} 次，答对 ${data.practiceCorrect} 次',
    };

    return Card.filled(
      color: colorScheme.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.xLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 36, color: colorScheme.onPrimaryContainer),
            const SizedBox(height: AppSpacing.large),
            Text(
              value,
              style: Theme.of(context).textTheme.displaySmall?.copyWith(
                    color: colorScheme.onPrimaryContainer,
                  ),
            ),
            Text(
              _metricTitle(metric),
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    color: colorScheme.onPrimaryContainer,
                  ),
            ),
            const SizedBox(height: AppSpacing.small),
            Text(
              description,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colorScheme.onPrimaryContainer,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

String _metricTitle(StatsMetric metric) {
  return switch (metric) {
    StatsMetric.errors => '错题',
    StatsMetric.mastered => '掌握情况',
    StatsMetric.accuracy => '练习正确率',
  };
}
