import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences_repository.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/app_preferences_mapper.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final localAppPreferencesStoreProvider = Provider<LocalAppPreferencesStore>(
  (ref) => LocalAppPreferencesStore(),
);

final localAppPreferencesRepositoryProvider =
    Provider<AppPreferencesRepository>((ref) {
  return LocalAppPreferencesRepository(
    ref.watch(localAppPreferencesStoreProvider),
  );
});

class LocalAppPreferencesRepository implements AppPreferencesRepository {
  const LocalAppPreferencesRepository(this.store);

  final LocalAppPreferencesStore store;

  @override
  Future<AppPreferences> read() => store.read();

  @override
  Future<void> save(AppPreferences preferences) => store.save(preferences);
}

class LocalAppPreferencesStore {
  LocalAppPreferencesStore({Future<Directory> Function()? supportDirectory})
      : _supportDirectory = supportDirectory ?? getApplicationSupportDirectory;

  static const _mapper = AppPreferencesMapper();
  final Future<Directory> Function() _supportDirectory;

  Future<AppPreferences> read() async {
    final file = await _file();
    if (!await file.exists()) return const AppPreferences();
    try {
      final payload = jsonDecode(await file.readAsString());
      final record = AppPreferencesRecord.fromJson(
        requireJsonObject(payload, context: 'app preferences record'),
      );
      return _mapper.fromRecord(record);
    } catch (_) {
      return const AppPreferences();
    }
  }

  Future<void> save(AppPreferences preferences) async {
    final file = await _file();
    await file.writeAsString(
      jsonEncode(_mapper.toRecord(preferences).toJson()),
      flush: true,
    );
  }

  Future<void> clear() async {
    final file = await _file();
    if (await file.exists()) await file.delete();
  }

  Future<File> _file() async {
    final root = await _supportDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'settings'));
    await directory.create(recursive: true);
    return File(p.join(directory.path, 'app_preferences_v1.json'));
  }
}
