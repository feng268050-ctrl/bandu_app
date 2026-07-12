import 'package:bandu_wrong_notebook/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/features/stats/data/stats_api_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final library = ref.watch(libraryControllerProvider);
    final stats = ref.watch(statsOverviewProvider);
    final cachedItemCount = library.valueOrNull?.length ?? 0;
    final totalErrors = stats.valueOrNull?.totalErrors ?? cachedItemCount;
    final masteredCount = stats.valueOrNull?.masteredCount ?? 0;
    final practiceAccuracy = stats.valueOrNull?.practiceAccuracy ?? 0;

    return Scaffold(
      appBar: AppBar(title: const Text('首页')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('今日', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _MetricCard(
                  label: '错题',
                  value: totalErrors.toString(),
                  icon: Icons.library_books,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _MetricCard(
                  label: '已掌握',
                  value: masteredCount.toString(),
                  icon: Icons.quiz,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _MetricCard(
            label: '练习正确率',
            value: '${(practiceAccuracy * 100).toStringAsFixed(0)}%',
            icon: Icons.trending_up,
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: () => context.go('/capture'),
            icon: const Icon(Icons.add_a_photo_outlined),
            label: const Text('拍题'),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => context.go('/library'),
            icon: const Icon(Icons.search),
            label: const Text('查看错题本'),
          ),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon),
            const SizedBox(height: 16),
            Text(value, style: Theme.of(context).textTheme.headlineMedium),
            Text(label),
          ],
        ),
      ),
    );
  }
}
