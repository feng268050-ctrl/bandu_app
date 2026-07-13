import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/tutor_session_cache_mapper.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class LocalTutorSessionStore {
  const LocalTutorSessionStore({
    this.mapper = const TutorSessionCacheMapper(),
  });

  final TutorSessionCacheMapper mapper;

  Future<List<TutorSession>> load() async {
    final file = await _file();
    if (!await file.exists()) return const [];
    final decoded = jsonDecode(await file.readAsString());
    if (decoded is! List) return const [];
    final sessions = decoded
        .map((item) => requireJsonObject(item, context: 'tutor session'))
        .map(TutorSessionRecord.fromJson)
        .map(mapper.fromRecord)
        .toList();
    sessions.sort((left, right) => right.updatedAt.compareTo(left.updatedAt));
    return sessions;
  }

  Future<void> save(TutorSession session) async {
    final sessions = await load();
    final updated = [
      session,
      ...sessions.where((item) => item.id != session.id),
    ];
    await _write(updated);
  }

  Future<void> delete(String sessionId) async {
    final sessions = await load();
    await _write(sessions.where((item) => item.id != sessionId).toList());
  }

  Future<String> persistAttachment(String sourcePath) async {
    final source = File(sourcePath);
    final root = await getApplicationDocumentsDirectory();
    final directory =
        Directory(p.join(root.path, 'bandu', 'tutor', 'attachments'));
    await directory.create(recursive: true);
    final extension =
        p.extension(sourcePath).isEmpty ? '.jpg' : p.extension(sourcePath);
    final target = File(
      p.join(
          directory.path, '${DateTime.now().microsecondsSinceEpoch}$extension'),
    );
    await source.copy(target.path);
    return target.path;
  }

  Future<void> clear() async {
    final file = await _file();
    if (await file.exists()) await file.delete();
  }

  Future<void> _write(List<TutorSession> sessions) async {
    final file = await _file();
    await file.writeAsString(
      jsonEncode(
        sessions.map(mapper.toRecord).map((item) => item.toJson()).toList(),
      ),
      flush: true,
    );
  }

  Future<File> _file() async {
    final root = await getApplicationSupportDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'tutor'));
    await directory.create(recursive: true);
    return File(p.join(directory.path, 'sessions_v1.json'));
  }
}
