import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:bandu_wrong_notebook/framework/network/services/error_item_api_service.dart';
import 'package:bandu_wrong_notebook/conversion/api/error_items/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final remoteErrorItemRepositoryProvider = Provider<ErrorItemRepository>((ref) {
  return RemoteErrorItemRepository(
    apiService: ref.watch(errorItemApiServiceProvider),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    mapper: const ErrorItemDtoMapper(),
    cacheMapper: const ErrorItemCacheMapper(),
  );
});

class RemoteErrorItemRepository implements ErrorItemRepository {
  const RemoteErrorItemRepository({
    required this.apiService,
    required this.cacheDatabase,
    required this.mapper,
    required this.cacheMapper,
  });

  final ErrorItemApiService apiService;
  final AppCacheDatabase cacheDatabase;
  final ErrorItemDtoMapper mapper;
  final ErrorItemCacheMapper cacheMapper;

  @override
  Future<List<ErrorItemSummary>> fetchErrorItems() async {
    try {
      final items = mapper.summaryListFromDtos(
        await apiService.fetchErrorItems(),
      );
      await cacheDatabase.replaceErrorItems(
        items.map(cacheMapper.summaryToCache).toList(),
      );
      return items;
    } catch (error) {
      if (error is! AppFailure || !error.allowsOfflineFallback) {
        rethrow;
      }
      final cached = await cacheDatabase.readErrorItems();
      if (cached.isEmpty) {
        rethrow;
      }
      return cached.map(cacheMapper.summaryFromCache).toList();
    }
  }

  @override
  Future<ErrorItemDetail> fetchErrorItem(String id) async {
    try {
      final item = mapper.detailFromDto(await apiService.fetchErrorItem(id));
      await cacheDatabase
          .upsertErrorItemDetail(cacheMapper.detailToCache(item));
      return item;
    } catch (error) {
      if (error is! AppFailure || !error.allowsOfflineFallback) {
        rethrow;
      }
      final cached = await cacheDatabase.readErrorItemDetail(id);
      if (cached != null) {
        return cacheMapper.detailFromCache(cached);
      }
      rethrow;
    }
  }

  @override
  Future<ErrorItemDetail> updateErrorItem(
    String id,
    ErrorItemUpdate update,
  ) async {
    final item = mapper.detailFromDto(
      await apiService.updateErrorItem(id, mapper.updateRequest(update)),
    );
    await cacheDatabase.upsertErrorItemDetail(cacheMapper.detailToCache(item));
    return item;
  }

  @override
  Future<void> deleteErrorItem(String id) async {
    await apiService.deleteErrorItem(id);
    await cacheDatabase.deleteErrorItem(id);
  }
}
