import 'package:bandu_wrong_notebook/core/database/app_database.dart';
import 'package:bandu_wrong_notebook/features/library/data/error_item_api_service.dart';
import 'package:bandu_wrong_notebook/features/library/data/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemRepositoryProvider = Provider<ErrorItemRepository>((ref) {
  return RemoteErrorItemRepository(
    apiService: ref.watch(errorItemApiServiceProvider),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    mapper: const ErrorItemDtoMapper(),
  );
});

class RemoteErrorItemRepository implements ErrorItemRepository {
  const RemoteErrorItemRepository({
    required this.apiService,
    required this.cacheDatabase,
    required this.mapper,
  });

  final ErrorItemApiService apiService;
  final AppCacheDatabase cacheDatabase;
  final ErrorItemDtoMapper mapper;

  @override
  Future<List<ErrorItemSummary>> fetchErrorItems() async {
    try {
      final items = mapper.summaryListFromJson(
        await apiService.fetchErrorItems(),
      );
      await cacheDatabase.upsertErrorItems(
        items.map(mapper.summaryToCache).toList(),
      );
      return items;
    } catch (_) {
      final cached = await cacheDatabase.readErrorItems();
      return cached.map(mapper.summaryFromCache).toList();
    }
  }

  @override
  Future<ErrorItemDetail> fetchErrorItem(String id) async {
    return mapper.detailFromJson(await apiService.fetchErrorItem(id));
  }
}
