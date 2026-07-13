import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final appCacheDatabaseProvider = Provider<AppCacheDatabase>((ref) {
  final database = FileAppCacheDatabase();
  ref.onDispose(database.dispose);
  return database;
});

class CachedSubject {
  const CachedSubject({required this.id, required this.name, this.updatedAt});

  final String id;
  final String name;
  final DateTime? updatedAt;
}

class CachedErrorItem {
  const CachedErrorItem({
    required this.id,
    required this.title,
    required this.subjectName,
    required this.updatedAt,
    this.mastered = false,
  });

  factory CachedErrorItem.fromJson(Map<String, Object?> json) {
    return CachedErrorItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      updatedAt:
          DateTime.tryParse(json['updatedAt']?.toString() ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      mastered: json['mastered'] == true,
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final DateTime updatedAt;
  final bool mastered;

  Map<String, Object?> toJson() {
    return {
      'id': id,
      'title': title,
      'subjectName': subjectName,
      'updatedAt': updatedAt.toIso8601String(),
      'mastered': mastered,
    };
  }
}

class CachedErrorItemDetail {
  const CachedErrorItemDetail({
    required this.id,
    required this.title,
    required this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.masteryLevel = 0,
    this.updatedAt,
  });

  factory CachedErrorItemDetail.fromJson(Map<String, Object?> json) {
    return CachedErrorItemDetail(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString() ?? '未分类',
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      masteryLevel: switch (json['masteryLevel']) {
        final int value => value,
        final num value => value.toInt(),
        final String value => int.tryParse(value) ?? 0,
        _ => 0,
      },
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? ''),
    );
  }

  final String id;
  final String title;
  final String subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final int masteryLevel;
  final DateTime? updatedAt;

  Map<String, Object?> toJson() {
    return {
      'id': id,
      'title': title,
      'subjectName': subjectName,
      'questionText': questionText,
      'answer': answer,
      'analysis': analysis,
      'masteryLevel': masteryLevel,
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}

class CachedKnowledgeTag {
  const CachedKnowledgeTag({
    required this.id,
    required this.name,
    this.parentId,
  });

  final String id;
  final String name;
  final String? parentId;
}

class SyncMetadata {
  const SyncMetadata({
    required this.key,
    required this.value,
    required this.updatedAt,
  });

  final String key;
  final String value;
  final DateTime updatedAt;
}

abstract interface class AppCacheDatabase {
  Stream<List<CachedErrorItem>> watchErrorItems();

  Future<List<CachedErrorItem>> readErrorItems();

  Future<void> upsertErrorItems(List<CachedErrorItem> items);

  Future<void> replaceErrorItems(List<CachedErrorItem> items);

  Future<CachedErrorItemDetail?> readErrorItemDetail(String id);

  Future<void> upsertErrorItemDetail(CachedErrorItemDetail detail);

  Future<void> deleteErrorItem(String id);

  Future<void> clear();
}

class FileAppCacheDatabase implements AppCacheDatabase {
  final Map<String, CachedErrorItem> _errorItems = {};
  final Map<String, CachedErrorItemDetail> _errorItemDetails = {};
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
  Future<void> clear() async {
    await _ensureLoaded();
    _errorItems.clear();
    _errorItemDetails.clear();
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
    final directory = await getApplicationSupportDirectory();
    final cacheDirectory = Directory(p.join(directory.path, 'bandu', 'cache'));
    await cacheDirectory.create(recursive: true);
    _file = File(p.join(cacheDirectory.path, 'app_cache_v1.json'));

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
    } catch (_) {
      _errorItems.clear();
      _errorItemDetails.clear();
    }
  }

  Future<void> _persist() async {
    final file = _file;
    if (file == null) {
      return;
    }

    await file.writeAsString(
      jsonEncode({
        'schemaVersion': 1,
        'errorItems': _sortedItems().map((item) => item.toJson()).toList(),
        'errorItemDetails': _errorItemDetails.values
            .map((item) => item.toJson())
            .toList(),
      }),
      flush: true,
    );
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

class MemoryAppCacheDatabase implements AppCacheDatabase {
  final Map<String, CachedErrorItem> _errorItems = {};
  final Map<String, CachedErrorItemDetail> _errorItemDetails = {};
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
  Future<void> clear() async {
    _errorItems.clear();
    _errorItemDetails.clear();
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
