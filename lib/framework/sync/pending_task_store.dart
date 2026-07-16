import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/pending_task_cache_mapper.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final pendingTaskStoreProvider = Provider<PendingTaskStore>((ref) {
  return FilePendingTaskStore();
});

abstract interface class PendingTaskStore {
  Future<List<PendingTaskCacheRecord>> read();

  Future<void> write(List<PendingTaskCacheRecord> tasks);

  Future<void> clear();
}

class FilePendingTaskStore implements PendingTaskStore {
  FilePendingTaskStore({Future<Directory> Function()? supportDirectory})
      : _supportDirectory = supportDirectory ?? getApplicationSupportDirectory;

  final Future<Directory> Function() _supportDirectory;
  File? _file;

  @override
  Future<List<PendingTaskCacheRecord>> read() async {
    final file = await _resolveFile();
    await _recoverTemporaryFile(file);
    if (!await file.exists()) {
      return const [];
    }

    try {
      final text = await file.readAsString();
      if (text.trim().isEmpty) {
        return const [];
      }
      final root = requireJsonObject(
        jsonDecode(text),
        context: 'pending task store',
      );
      final tasks = root['tasks'];
      if (tasks is! List<Object?>) {
        return const [];
      }
      return tasks
          .whereType<Map<String, Object?>>()
          .map(PendingTaskCacheRecord.fromJson)
          .toList(growable: false);
    } catch (_) {
      await _deleteIfExists(file);
      await _deleteIfExists(File('${file.path}.tmp'));
      return const [];
    }
  }

  @override
  Future<void> write(List<PendingTaskCacheRecord> tasks) async {
    final file = await _resolveFile();
    final temporary = File('${file.path}.tmp');
    await temporary.writeAsString(
      jsonEncode({
        'schemaVersion': 1,
        'tasks': tasks.map((task) => task.toJson()).toList(),
      }),
      flush: true,
    );
    await _replaceFile(temporary, file);
  }

  @override
  Future<void> clear() async {
    final file = await _resolveFile();
    await _deleteIfExists(file);
    await _deleteIfExists(File('${file.path}.tmp'));
  }

  Future<File> _resolveFile() async {
    final existing = _file;
    if (existing != null) {
      return existing;
    }
    final support = await _supportDirectory();
    final directory = Directory(p.join(support.path, 'bandu', 'sync'));
    await directory.create(recursive: true);
    return _file = File(p.join(directory.path, 'pending_tasks_v1.json'));
  }

  Future<void> _deleteIfExists(File file) async {
    if (await file.exists()) {
      await file.delete();
    }
  }

  Future<void> _recoverTemporaryFile(File target) async {
    final temporary = File('${target.path}.tmp');
    if (!await temporary.exists()) {
      return;
    }
    try {
      final root = requireJsonObject(
        jsonDecode(await temporary.readAsString()),
        context: 'pending task temporary store',
      );
      if (root['tasks'] is! List<Object?>) {
        throw const FormatException('pending task list missing');
      }
      await _replaceFile(temporary, target);
    } catch (_) {
      await _deleteIfExists(temporary);
    }
  }

  Future<void> _replaceFile(File source, File target) async {
    try {
      await source.rename(target.path);
    } on FileSystemException {
      await _deleteIfExists(target);
      await source.rename(target.path);
    }
  }
}

class MemoryPendingTaskStore implements PendingTaskStore {
  MemoryPendingTaskStore([List<PendingTaskCacheRecord> initial = const []])
      : _tasks = List.of(initial);

  List<PendingTaskCacheRecord> _tasks;

  @override
  Future<List<PendingTaskCacheRecord>> read() async => List.of(_tasks);

  @override
  Future<void> write(List<PendingTaskCacheRecord> tasks) async {
    _tasks = List.of(tasks);
  }

  @override
  Future<void> clear() async {
    _tasks = [];
  }
}
