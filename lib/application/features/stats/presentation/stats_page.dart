import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
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
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => Center(child: Text(error.toString())),
        data: (data) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _StatsTile(label: '错题总数', value: data.totalErrors.toString()),
              _StatsTile(label: '已掌握', value: data.masteredCount.toString()),
              _StatsTile(
                label: '掌握率',
                value: '${(data.masteryRate * 100).toStringAsFixed(0)}%',
              ),
              _StatsTile(label: '练习次数', value: data.practiceTotal.toString()),
              _StatsTile(
                label: '练习正确率',
                value: '${(data.practiceAccuracy * 100).toStringAsFixed(0)}%',
              ),
            ],
          );
        },
      ),
    );
  }
}

class _StatsTile extends StatelessWidget {
  const _StatsTile({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text(label),
        trailing: Text(value, style: Theme.of(context).textTheme.titleLarge),
      ),
    );
  }
}
