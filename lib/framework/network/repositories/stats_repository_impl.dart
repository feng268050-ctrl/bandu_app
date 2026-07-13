import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_overview.dart';
import 'package:bandu_wrong_notebook/application/features/stats/domain/stats_repository.dart';
import 'package:bandu_wrong_notebook/conversion/api/stats/stats_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/stats_api_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteStatsRepositoryProvider = Provider<StatsRepository>((ref) {
  return RemoteStatsRepository(
    apiService: ref.watch(statsApiServiceProvider),
    mapper: const StatsDtoMapper(),
  );
});

class RemoteStatsRepository implements StatsRepository {
  const RemoteStatsRepository({
    required this.apiService,
    required this.mapper,
  });

  final StatsApiService apiService;
  final StatsDtoMapper mapper;

  @override
  Future<StatsOverview> fetchOverview() async {
    return mapper.overviewFromJson(await apiService.fetchOverview());
  }
}
