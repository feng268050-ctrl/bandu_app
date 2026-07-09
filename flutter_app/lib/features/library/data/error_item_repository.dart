import 'package:bandu_wrong_notebook/core/database/app_database.dart';
import 'package:bandu_wrong_notebook/core/network/api_client.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemRepositoryProvider = Provider<ErrorItemRepository>((ref) {
  return RemoteErrorItemRepository(
    apiClient: ref.watch(apiClientProvider),
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
  );
});

abstract interface class ErrorItemRepository {
  Future<List<ErrorItemSummary>> fetchErrorItems();

  Future<ErrorItemDetail> fetchErrorItem(String id);
}

class RemoteErrorItemRepository implements ErrorItemRepository {
  const RemoteErrorItemRepository({
    required this.apiClient,
    required this.cacheDatabase,
  });

  final ApiClient apiClient;
  final AppCacheDatabase cacheDatabase;

  @override
  Future<List<ErrorItemSummary>> fetchErrorItems() async {
    try {
      final data = await apiClient.get<List<Object?>>('/error-items');
      final items = data
          .whereType<Map<String, Object?>>()
          .map(ErrorItemSummary.fromJson)
          .toList();
      await cacheDatabase.upsertErrorItems(items.map(_toCache).toList());
      return items;
    } catch (_) {
      final cached = await cacheDatabase.readErrorItems();
      return cached.map(_fromCache).toList();
    }
  }

  @override
  Future<ErrorItemDetail> fetchErrorItem(String id) async {
    final data = await apiClient.get<Map<String, Object?>>('/error-items/$id');
    return ErrorItemDetail.fromJson(data);
  }

  CachedErrorItem _toCache(ErrorItemSummary item) {
    return CachedErrorItem(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      updatedAt: item.updatedAt,
      mastered: item.mastered,
    );
  }

  ErrorItemSummary _fromCache(CachedErrorItem item) {
    return ErrorItemSummary(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      updatedAt: item.updatedAt,
      mastered: item.mastered,
    );
  }
}
