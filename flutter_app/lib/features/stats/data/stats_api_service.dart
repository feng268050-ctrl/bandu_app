import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/features/stats/domain/stats_overview.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final statsOverviewProvider = FutureProvider<StatsOverview>((ref) async {
  final data = await ref
      .watch(apiClientProvider)
      .get<Map<String, Object?>>('/stats/overview');
  return StatsOverview.fromJson(data);
});
