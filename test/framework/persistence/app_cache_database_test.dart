import 'dart:io';

import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as p;

void main() {
  final now = DateTime.utc(2026, 7, 16, 8);

  test('corrupted cache is deleted and rebuilt as empty', () async {
    final directory = await Directory.systemTemp.createTemp('bandu-cache-');
    addTearDown(() => directory.delete(recursive: true));
    final cacheDirectory = Directory(p.join(directory.path, 'bandu', 'cache'));
    await cacheDirectory.create(recursive: true);
    final file = File(p.join(cacheDirectory.path, 'app_cache_v1.json'));
    await file.writeAsString('{broken');
    final database = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    addTearDown(database.dispose);

    expect(await database.readErrorItems(), isEmpty);
    expect(await file.exists(), isFalse);

    await database.upsertErrorItems([
      CachedErrorItem(
        id: 'one',
        title: '题目',
        subjectName: '数学',
        updatedAt: now,
      ),
    ]);
    expect(await file.exists(), isTrue);
    expect((await database.readErrorItems()).single.id, 'one');
  });

  test('cache keeps the latest 500 items and expires old statistics', () async {
    final directory = await Directory.systemTemp.createTemp('bandu-cache-');
    addTearDown(() => directory.delete(recursive: true));
    final database = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    addTearDown(database.dispose);

    await database.replaceErrorItems([
      for (var index = 0; index < 510; index += 1)
        CachedErrorItem(
          id: 'item-$index',
          title: '题目 $index',
          subjectName: '数学',
          updatedAt: now.add(Duration(minutes: index)),
        ),
    ]);
    await database.upsertStatsOverview(
      CachedStatsOverview(
        period: 'week',
        totalErrors: 12,
        masteredCount: 5,
        masteryRate: 5 / 12,
        practiceTotal: 8,
        practiceCorrect: 6,
        practiceAccuracy: 0.75,
        cachedAt: now.subtract(const Duration(days: 31)),
      ),
    );

    final items = await database.readErrorItems();
    expect(items, hasLength(500));
    expect(items.first.id, 'item-509');
    expect(items.any((item) => item.id == 'item-0'), isFalse);
    expect(await database.readStatsOverview('week'), isNull);
  });

  test('cache values survive database re-instantiation', () async {
    final directory = await Directory.systemTemp.createTemp('bandu-cache-');
    addTearDown(() => directory.delete(recursive: true));
    final first = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    await first.upsertErrorItemDetail(
      CachedErrorItemDetail(
        id: 'detail-1',
        title: '几何题',
        subjectName: '数学',
        questionText: '求面积',
        updatedAt: now,
      ),
    );
    first.dispose();

    final reopened = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    addTearDown(reopened.dispose);

    expect((await reopened.readErrorItems()).single.id, 'detail-1');
    expect(
      (await reopened.readErrorItemDetail('detail-1'))?.questionText,
      '求面积',
    );
  });

  test('complete temporary cache is recovered after interrupted replacement',
      () async {
    final directory = await Directory.systemTemp.createTemp('bandu-cache-');
    addTearDown(() => directory.delete(recursive: true));
    final first = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    await first.upsertErrorItems([
      CachedErrorItem(
        id: 'recover-1',
        title: '待恢复题目',
        subjectName: '数学',
        updatedAt: now,
      ),
    ]);
    first.dispose();
    final file = File(
      p.join(directory.path, 'bandu', 'cache', 'app_cache_v1.json'),
    );
    await file.rename('${file.path}.tmp');

    final recovered = FileAppCacheDatabase(
      supportDirectory: () async => directory,
      clock: () => now,
    );
    addTearDown(recovered.dispose);

    expect((await recovered.readErrorItems()).single.id, 'recover-1');
    expect(await file.exists(), isTrue);
  });
}
