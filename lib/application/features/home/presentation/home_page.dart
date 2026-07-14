import 'package:bandu_wrong_notebook/application/features/capture/presentation/capture_controller.dart';
import 'package:bandu_wrong_notebook/application/features/home/presentation/widgets/home_metric_card.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_metric.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_period.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  StatsPeriod _period = StatsPeriod.today;

  @override
  Widget build(BuildContext context) {
    final stats = ref.watch(statsOverviewProvider(_period));
    final totalErrors = stats.valueOrNull?.totalErrors ?? 0;
    final masteredCount = stats.valueOrNull?.masteredCount ?? 0;
    final practiceTotal = stats.valueOrNull?.practiceTotal ?? 0;
    final practiceAccuracy = stats.valueOrNull?.practiceAccuracy ?? 0;
    final hasPractice = practiceTotal > 0;

    return Scaffold(
      appBar: AppBar(title: const Text('首页')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: PopupMenuButton<StatsPeriod>(
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
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _period.label,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const Icon(Icons.arrow_drop_down),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.medium),
          Row(
            children: [
              Expanded(
                child: HomeMetricCard(
                  label: '错题',
                  value: totalErrors.toString(),
                  icon: Icons.library_books_outlined,
                  onTap: () => _openDetails(StatsMetric.errors),
                ),
              ),
              const SizedBox(width: AppSpacing.medium),
              Expanded(
                child: HomeMetricCard(
                  label: '已掌握',
                  value: masteredCount.toString(),
                  icon: Icons.task_alt_outlined,
                  onTap: () => _openDetails(StatsMetric.mastered),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.medium),
          HomeMetricCard(
            label: '练习正确率',
            value: hasPractice
                ? '${(practiceAccuracy * 100).toStringAsFixed(0)}%'
                : '${_period.label}未开始练习',
            icon: Icons.trending_up,
            showLabel: hasPractice,
            onTap: () => _openDetails(StatsMetric.accuracy),
          ),
          const SizedBox(height: AppSpacing.xLarge),
          AppPrimaryButton(
            label: '拍题',
            expanded: true,
            icon: const Icon(Icons.camera_alt_outlined),
            onPressed: _takePhotoFromHome,
          ),
          const SizedBox(height: AppSpacing.medium),
          AppDefaultButton(
            label: '查看错题本',
            expanded: true,
            icon: const Icon(Icons.search),
            onPressed: () => context.go('/home/library'),
          ),
        ],
      ),
    );
  }

  void _openDetails(StatsMetric metric) {
    context.go(
      '/home/stats/${metric.routeValue}?period=${_period.apiValue}',
    );
  }

  Future<void> _takePhotoFromHome() async {
    final captured =
        await ref.read(captureControllerProvider.notifier).takePhoto();
    if (!mounted || !captured) {
      return;
    }
    context.go('/capture');
  }
}
