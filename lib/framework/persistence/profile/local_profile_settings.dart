import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/avatar_settings_mapper.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final localProfileSettingsStoreProvider = Provider<LocalProfileSettingsStore>((
  ref,
) {
  return LocalProfileSettingsStore();
});

final localAvatarRepositoryProvider = Provider<AvatarRepository>((ref) {
  return LocalAvatarRepository(
    store: ref.watch(localProfileSettingsStoreProvider),
    picker: ImagePicker(),
  );
});

class LocalAvatarRepository implements AvatarRepository {
  const LocalAvatarRepository({required this.store, required this.picker});

  final LocalProfileSettingsStore store;
  final ImagePicker picker;

  @override
  Future<AvatarSettings> readSettings() => store.readAvatarSettings();

  @override
  Future<String?> pickImage(AvatarImageSource source) async {
    final image = await picker.pickImage(
      source: source == AvatarImageSource.camera
          ? ImageSource.camera
          : ImageSource.gallery,
      imageQuality: 90,
      maxWidth: 1600,
    );
    return image?.path;
  }

  @override
  Future<AvatarSettings> saveColor(int colorValue) {
    return store.saveAvatarColor(colorValue);
  }

  @override
  Future<AvatarSettings> saveImage({
    required String sourcePath,
    required int fallbackColorValue,
  }) {
    return store.saveAvatarImage(
      sourcePath: sourcePath,
      fallbackColorValue: fallbackColorValue,
    );
  }
}

class LocalProfileSettingsStore {
  static const _mapper = AvatarSettingsMapper();

  Future<AvatarSettings> readAvatarSettings() async {
    final file = await _settingsFile();
    if (!await file.exists()) {
      return const AvatarSettings();
    }
    try {
      final payload = jsonDecode(await file.readAsString());
      final record = AvatarSettingsRecord.fromJson(
        requireJsonObject(payload, context: 'avatar settings record'),
      );
      final settings = _mapper.fromRecord(record);
      final imagePath = settings.imagePath;
      if (imagePath == null || await File(imagePath).exists()) {
        return settings;
      }
      return AvatarSettings(colorValue: settings.colorValue);
    } catch (_) {
      // A damaged preference file should not prevent the profile page opening.
    }
    return const AvatarSettings();
  }

  Future<AvatarSettings> saveAvatarColor(int colorValue) async {
    final settings = AvatarSettings(colorValue: colorValue);
    await _write(settings);
    return settings;
  }

  Future<AvatarSettings> saveAvatarImage({
    required String sourcePath,
    required int fallbackColorValue,
  }) async {
    final source = File(sourcePath);
    if (!await source.exists()) {
      throw StateError('选择的头像图片已不可用');
    }
    final root = await getApplicationDocumentsDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'profile'));
    await directory.create(recursive: true);
    final extension = p.extension(sourcePath).toLowerCase();
    final target = File(
      p.join(directory.path, 'avatar${extension.isEmpty ? '.jpg' : extension}'),
    );
    if (source.absolute.path != target.absolute.path) {
      await source.copy(target.path);
    }

    final settings = AvatarSettings(
      colorValue: fallbackColorValue,
      imagePath: target.path,
    );
    await _write(settings);
    return settings;
  }

  Future<void> clear() async {
    final file = await _settingsFile();
    if (await file.exists()) {
      await file.delete();
    }
  }

  Future<void> _write(AvatarSettings settings) async {
    final file = await _settingsFile();
    await file.writeAsString(
      jsonEncode(_mapper.toRecord(settings).toJson()),
      flush: true,
    );
  }

  Future<File> _settingsFile() async {
    final root = await getApplicationSupportDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'profile'));
    await directory.create(recursive: true);
    return File(p.join(directory.path, 'profile_settings_v1.json'));
  }
}
