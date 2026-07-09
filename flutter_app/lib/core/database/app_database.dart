import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

final appCacheDatabaseProvider = Provider<AppCacheDatabase>((ref) {
  final database = MemoryAppCacheDatabase();
  ref.onDispose(database.dispose);
  return database;
});

class CachedSubject {
  const CachedSubject({
    required this.id,
    required this.name,
    this.updatedAt,
  });

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

  final String id;
  final String title;
  final String subjectName;
  final DateTime updatedAt;
  final bool mastered;
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

  Future<void> clear();
}

class MemoryAppCacheDatabase implements AppCacheDatabase {
  final Map<String, CachedErrorItem> _errorItems = {};
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
  Future<void> clear() async {
    _errorItems.clear();
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
