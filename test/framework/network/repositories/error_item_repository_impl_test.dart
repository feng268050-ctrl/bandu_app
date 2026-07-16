import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/conversion/api/error_items/error_item_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/error_item_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/services/error_item_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final now = DateTime.utc(2026, 7, 16);

  test('error item list falls back only for offline-capable failures',
      () async {
    final cache = MemoryAppCacheDatabase();
    addTearDown(cache.dispose);
    await cache.replaceErrorItems([
      CachedErrorItem(
        id: 'error-1',
        title: '缓存题目',
        subjectName: '数学',
        updatedAt: now,
      ),
    ]);
    final service = _FakeErrorItemApiService(
      error: const AppFailure(
        code: 'NETWORK_UNAVAILABLE',
        message: '当前网络不可用',
      ),
    );
    final repository = _repository(service, cache);

    final items = await repository.fetchErrorItems();

    expect(items.single.title, '缓存题目');
    expect(items.single.isFromCache, isTrue);
  });

  test('error item detail is marked when read from offline cache', () async {
    final cache = MemoryAppCacheDatabase();
    addTearDown(cache.dispose);
    await cache.upsertErrorItemDetail(
      CachedErrorItemDetail(
        id: 'error-1',
        title: '缓存题目',
        subjectName: '数学',
        questionText: '1 + 1',
        answer: '2',
        updatedAt: now,
      ),
    );
    final repository = _repository(
      _FakeErrorItemApiService(
        error: const AppFailure(
          code: 'CONNECTION_TIMEOUT',
          message: '连接超时',
        ),
      ),
      cache,
    );

    final detail = await repository.fetchErrorItem('error-1');

    expect(detail.answer, '2');
    expect(detail.isFromCache, isTrue);
  });

  test('error item repository does not hide mapper or programming failures',
      () async {
    final cache = MemoryAppCacheDatabase();
    addTearDown(cache.dispose);
    await cache.replaceErrorItems([
      CachedErrorItem(
        id: 'error-1',
        title: '缓存题目',
        subjectName: '数学',
        updatedAt: now,
      ),
    ]);
    final repository = _repository(
      _FakeErrorItemApiService(error: const FormatException('bad payload')),
      cache,
    );

    await expectLater(
      repository.fetchErrorItems(),
      throwsA(isA<FormatException>()),
    );
  });
}

RemoteErrorItemRepository _repository(
  ErrorItemApiService service,
  AppCacheDatabase cache,
) {
  return RemoteErrorItemRepository(
    apiService: service,
    cacheDatabase: cache,
    mapper: const ErrorItemDtoMapper(),
    cacheMapper: const ErrorItemCacheMapper(),
  );
}

class _FakeErrorItemApiService implements ErrorItemApiService {
  _FakeErrorItemApiService({this.error});

  final Object? error;

  Never _throw() => throw error ?? StateError('unexpected service call');

  @override
  Future<void> deleteErrorItem(String id) async => _throw();

  @override
  Future<ErrorItemDetailDto> fetchErrorItem(String id) async => _throw();

  @override
  Future<List<ErrorItemSummaryDto>> fetchErrorItems() async => _throw();

  @override
  Future<ErrorItemDetailDto> updateErrorItem(
    String id,
    ErrorItemUpdateRequestDto request,
  ) async =>
      _throw();
}
