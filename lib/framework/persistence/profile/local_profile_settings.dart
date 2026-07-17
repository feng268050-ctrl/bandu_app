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

  @override
  Future<String?> pickWallpaper() async {
    final image = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 90,
      maxWidth: 2400,
    );
    return image?.path;
  }

  @override
  Future<AvatarSettings> saveWallpaper(String sourcePath) {
    return store.saveWallpaper(sourcePath);
  }

  @override
  Future<AvatarSettings> removeWallpaper() {
    return store.removeWallpaper();
  }
}

class LocalProfileSettingsStore {
  LocalProfileSettingsStore({
    Future<Directory> Function()? supportDirectory,
    Future<Directory> Function()? documentsDirectory,
  })  : _supportDirectory = supportDirectory ?? getApplicationSupportDirectory,
        _documentsDirectory =
            documentsDirectory ?? getApplicationDocumentsDirectory;

  static const _mapper = AvatarSettingsMapper();
  final Future<Directory> Function() _supportDirectory;
  final Future<Directory> Function() _documentsDirectory;

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
      final wallpaperPath = settings.wallpaperPath;
      return AvatarSettings(
        colorValue: settings.colorValue,
        imagePath: imagePath != null && await File(imagePath).exists()
            ? imagePath
            : null,
        wallpaperPath:
            wallpaperPath != null && await File(wallpaperPath).exists()
                ? wallpaperPath
                : null,
      );
    } catch (_) {
      // A damaged preference file should not prevent the profile page opening.
    }
    return const AvatarSettings();
  }

  Future<AvatarSettings> saveAvatarColor(int colorValue) async {
    final current = await readAvatarSettings();
    final settings = AvatarSettings(
      colorValue: colorValue,
      wallpaperPath: current.wallpaperPath,
    );
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
    final current = await readAvatarSettings();
    final directory = await _profileDocumentsDirectory();
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
      wallpaperPath: current.wallpaperPath,
    );
    await _write(settings);
    return settings;
  }

  Future<AvatarSettings> saveWallpaper(String sourcePath) async {
    final source = File(sourcePath);
    if (!await source.exists()) {
      throw StateError('选择的壁纸图片已不可用');
    }
    final current = await readAvatarSettings();
    final directory = await _profileDocumentsDirectory();
    final extension = p.extension(sourcePath).toLowerCase();
    final target = File(
      p.join(
        directory.path,
        'wallpaper${extension.isEmpty ? '.jpg' : extension}',
      ),
    );
    if (source.absolute.path != target.absolute.path) {
      await source.copy(target.path);
    }
    final previousPath = current.wallpaperPath;
    if (previousPath != null && previousPath != target.path) {
      await _deleteProfileFile(previousPath);
    }
    final settings = AvatarSettings(
      colorValue: current.colorValue,
      imagePath: current.imagePath,
      wallpaperPath: target.path,
    );
    await _write(settings);
    return settings;
  }

  Future<AvatarSettings> removeWallpaper() async {
    final current = await readAvatarSettings();
    final wallpaperPath = current.wallpaperPath;
    if (wallpaperPath != null) {
      await _deleteProfileFile(wallpaperPath);
    }
    final settings = AvatarSettings(
      colorValue: current.colorValue,
      imagePath: current.imagePath,
    );
    await _write(settings);
    return settings;
  }

  Future<void> clear() async {
    final supportRoot = await _supportDirectory();
    final supportProfile = Directory(
      p.join(supportRoot.path, 'bandu', 'profile'),
    );
    if (await supportProfile.exists()) {
      await supportProfile.delete(recursive: true);
    }
    final documentsProfile = await _profileDocumentsDirectory(
      create: false,
    );
    if (await documentsProfile.exists()) {
      await documentsProfile.delete(recursive: true);
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
    final root = await _supportDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'profile'));
    await directory.create(recursive: true);
    return File(p.join(directory.path, 'profile_settings_v1.json'));
  }

  Future<Directory> _profileDocumentsDirectory({bool create = true}) async {
    final root = await _documentsDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'profile'));
    if (create) {
      await directory.create(recursive: true);
    }
    return directory;
  }

  Future<void> _deleteProfileFile(String filePath) async {
    final directory = await _profileDocumentsDirectory(create: false);
    final normalizedRoot = p.normalize(directory.absolute.path);
    final normalizedFile = p.normalize(File(filePath).absolute.path);
    if (!p.isWithin(normalizedRoot, normalizedFile)) return;
    final file = File(normalizedFile);
    if (await file.exists()) {
      await file.delete();
    }
  }
}
