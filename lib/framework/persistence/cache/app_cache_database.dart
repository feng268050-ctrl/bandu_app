import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final appCacheDatabaseProvider = Provider<AppCacheDatabase>((ref) {
  final database = FileAppCacheDatabase();
  ref.onDispose(database.dispose);
  return database;
});

abstract interface class AppCacheDatabase {
  Stream<List<CachedErrorItem>> watchErrorItems();

  Future<List<CachedErrorItem>> readErrorItems();

  Future<void> upsertErrorItems(List<CachedErrorItem> items);

  Future<void> replaceErrorItems(List<CachedErrorItem> items);

  Future<CachedErrorItemDetail?> readErrorItemDetail(String id);

  Future<void> upsertErrorItemDetail(CachedErrorItemDetail detail);

  Future<CachedStatsOverview?> readStatsOverview(String period);

  Future<void> upsertStatsOverview(CachedStatsOverview overview);

  Future<void> deleteErrorItem(String id);

  Future<void> clear();
}

class FileAppCacheDatabase implements AppCacheDatabase {
  FileAppCacheDatabase({
    Future<Directory> Function()? supportDirectory,
    DateTime Function()? clock,
  })  : _supportDirectory = supportDirectory ?? getApplicationSupportDirectory,
        _clock = clock ?? DateTime.now;

  static const _maxErrorItems = 500;
  static const _statsRetention = Duration(days: 30);

  final Future<Directory> Function() _supportDirectory;
  final DateTime Function() _clock;
  final Map<String, CachedErrorItem> _errorItems = {};
  final Map<String, CachedErrorItemDetail> _errorItemDetails = {};
  final Map<String, CachedStatsOverview> _statsOverviews = {};
  final StreamController<List<CachedErrorItem>> _controller =
      StreamController<List<CachedErrorItem>>.broadcast();
  Future<void>? _loadFuture;
  File? _file;

  @override
  Stream<List<CachedErrorItem>> watchErrorItems() {
    Future<void>.microtask(() async {
      await _ensureLoaded();
      _emit();
    });
    return _controller.stream;
  }

  @override
  Future<List<CachedErrorItem>> readErrorItems() async {
    await _ensureLoaded();
    return _sortedItems();
  }

  @override
  Future<void> upsertErrorItems(List<CachedErrorItem> items) async {
    await _ensureLoaded();
    for (final item in items) {
      _errorItems[item.id] = item;
    }
    await _persist();
    _emit();
  }

  @override
  Future<void> replaceErrorItems(List<CachedErrorItem> items) async {
    await _ensureLoaded();
    final ids = items.map((item) => item.id).toSet();
    _errorItems
      ..clear()
      ..addEntries(items.map((item) => MapEntry(item.id, item)));
    _errorItemDetails.removeWhere((id, _) => !ids.contains(id));
    await _persist();
    _emit();
  }

  @override
  Future<CachedErrorItemDetail?> readErrorItemDetail(String id) async {
    await _ensureLoaded();
    return _errorItemDetails[id];
  }

  @override
  Future<void> upsertErrorItemDetail(CachedErrorItemDetail detail) async {
    await _ensureLoaded();
    _errorItemDetails[detail.id] = detail;
    _errorItems[detail.id] = CachedErrorItem(
      id: detail.id,
      title: detail.title,
      subjectName: detail.subjectName,
      updatedAt: detail.updatedAt ?? DateTime.now(),
    );
    await _persist();
    _emit();
  }

  @override
  Future<void> deleteErrorItem(String id) async {
    await _ensureLoaded();
    _errorItems.remove(id);
    _errorItemDetails.remove(id);
    await _persist();
    _emit();
  }

  @override
  Future<CachedStatsOverview?> readStatsOverview(String period) async {
    await _ensureLoaded();
    return _statsOverviews[period];
  }

  @override
  Future<void> upsertStatsOverview(CachedStatsOverview overview) async {
    await _ensureLoaded();
    _statsOverviews[overview.period] = overview;
    await _persist();
  }

  @override
  Future<void> clear() async {
    await _ensureLoaded();
    _errorItems.clear();
    _errorItemDetails.clear();
    _statsOverviews.clear();
    await _persist();
    _emit();
  }

  void dispose() {
    _controller.close();
  }

  Future<void> _ensureLoaded() {
    return _loadFuture ??= _load();
  }

  Future<void> _load() async {
    final directory = await _supportDirectory();
    final cacheDirectory = Directory(p.join(directory.path, 'bandu', 'cache'));
    await cacheDirectory.create(recursive: true);
    _file = File(p.join(cacheDirectory.path, 'app_cache_v1.json'));

    await _recoverTemporaryFile(_file!);

    if (!await _file!.exists()) {
      return;
    }

    try {
      final text = await _file!.readAsString();
      if (text.trim().isEmpty) {
        return;
      }

      final json = jsonDecode(text);
      if (json is! Map<String, Object?>) {
        return;
      }

      final items = json['errorItems'];
      if (items is List<Object?>) {
        for (final item in items.whereType<Map<String, Object?>>()) {
          final cached = CachedErrorItem.fromJson(item);
          if (cached.id.isNotEmpty) {
            _errorItems[cached.id] = cached;
          }
        }
      }

      final details = json['errorItemDetails'];
      if (details is List<Object?>) {
        for (final item in details.whereType<Map<String, Object?>>()) {
          final cached = CachedErrorItemDetail.fromJson(item);
          if (cached.id.isNotEmpty) {
            _errorItemDetails[cached.id] = cached;
          }
        }
      }
      final stats = json['statsOverviews'];
      if (stats is List<Object?>) {
        for (final item in stats.whereType<Map<String, Object?>>()) {
          final cached = CachedStatsOverview.fromJson(item);
          if (cached.period.isNotEmpty) {
            _statsOverviews[cached.period] = cached;
          }
        }
      }
      _prune();
    } catch (_) {
      _errorItems.clear();
      _errorItemDetails.clear();
      _statsOverviews.clear();
      if (await _file!.exists()) {
        await _file!.delete();
      }
      final temporary = File('${_file!.path}.tmp');
      if (await temporary.exists()) {
        await temporary.delete();
      }
    }
  }

  Future<void> _persist() async {
    final file = _file;
    if (file == null) {
      return;
    }

    _prune();
    final temporary = File('${file.path}.tmp');
    await temporary.writeAsString(
      jsonEncode({
        'schemaVersion': 2,
        'errorItems': _sortedItems().map((item) => item.toJson()).toList(),
        'errorItemDetails':
            _errorItemDetails.values.map((item) => item.toJson()).toList(),
        'statsOverviews':
            _statsOverviews.values.map((item) => item.toJson()).toList(),
      }),
      flush: true,
    );
    await _replaceFile(temporary, file);
  }

  void _emit() {
    if (!_controller.isClosed) {
      _controller.add(_sortedItems());
    }
  }

  List<CachedErrorItem> _sortedItems() {
    final items = _errorItems.values.toList();
    items.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
    return items;
  }

  void _prune() {
    final sorted = _sortedItems();
    for (final item in sorted.skip(_maxErrorItems)) {
      _errorItems.remove(item.id);
      _errorItemDetails.remove(item.id);
    }
    final cutoff = _clock().subtract(_statsRetention);
    _statsOverviews.removeWhere((_, item) => item.cachedAt.isBefore(cutoff));
  }

  Future<void> _recoverTemporaryFile(File target) async {
    final temporary = File('${target.path}.tmp');
    if (!await temporary.exists()) {
      return;
    }
    try {
      final decoded = jsonDecode(await temporary.readAsString());
      if (decoded is! Map<String, Object?>) {
        throw const FormatException('cache root must be an object');
      }
      await _replaceFile(temporary, target);
    } catch (_) {
      await temporary.delete();
    }
  }

  Future<void> _replaceFile(File source, File target) async {
    try {
      await source.rename(target.path);
    } on FileSystemException {
      if (await target.exists()) {
        await target.delete();
      }
      await source.rename(target.path);
    }
  }
}

class MemoryAppCacheDatabase implements AppCacheDatabase {
  final Map<String, CachedErrorItem> _errorItems = {};
  final Map<String, CachedErrorItemDetail> _errorItemDetails = {};
  final Map<String, CachedStatsOverview> _statsOverviews = {};
  final StreamController<List<CachedErrorItem>> _controller =
      StreamController<List<CachedErrorItem>>.broadcast();

  @override
  Stream<List<CachedErrorItem>> watchErrorItems() {
    Future<void>.microtask(_emit);
    return _controller.stream;
  }

  @override
  Future<List<CachedErrorItem>> readErrorItems() async {
    return _sortedItems();
  }

  @override
  Future<void> upsertErrorItems(List<CachedErrorItem> items) async {
    for (final item in items) {
      _errorItems[item.id] = item;
    }
    _emit();
  }

  @override
  Future<void> replaceErrorItems(List<CachedErrorItem> items) async {
    final ids = items.map((item) => item.id).toSet();
    _errorItems
      ..clear()
      ..addEntries(items.map((item) => MapEntry(item.id, item)));
    _errorItemDetails.removeWhere((id, _) => !ids.contains(id));
    _emit();
  }

  @override
  Future<CachedErrorItemDetail?> readErrorItemDetail(String id) async {
    return _errorItemDetails[id];
  }

  @override
  Future<void> upsertErrorItemDetail(CachedErrorItemDetail detail) async {
    _errorItemDetails[detail.id] = detail;
    _errorItems[detail.id] = CachedErrorItem(
      id: detail.id,
      title: detail.title,
      subjectName: detail.subjectName,
      updatedAt: detail.updatedAt ?? DateTime.now(),
    );
    _emit();
  }

  @override
  Future<void> deleteErrorItem(String id) async {
    _errorItems.remove(id);
    _errorItemDetails.remove(id);
    _emit();
  }

  @override
  Future<CachedStatsOverview?> readStatsOverview(String period) async {
    return _statsOverviews[period];
  }

  @override
  Future<void> upsertStatsOverview(CachedStatsOverview overview) async {
    _statsOverviews[overview.period] = overview;
  }

  @override
  Future<void> clear() async {
    _errorItems.clear();
    _errorItemDetails.clear();
    _statsOverviews.clear();
    _emit();
  }

  void dispose() {
    _controller.close();
  }

  void _emit() {
    if (!_controller.isClosed) {
      _controller.add(_sortedItems());
    }
  }

  List<CachedErrorItem> _sortedItems() {
    final items = _errorItems.values.toList();
    items.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
    return items;
  }
}
