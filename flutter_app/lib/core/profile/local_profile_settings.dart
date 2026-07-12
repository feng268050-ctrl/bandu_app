import 'dart:convert';
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final localProfileSettingsStoreProvider = Provider<LocalProfileSettingsStore>((
  ref,
) {
  return LocalProfileSettingsStore();
});

final avatarSettingsProvider =
    AsyncNotifierProvider<AvatarSettingsController, AvatarSettings>(
      AvatarSettingsController.new,
    );

class AvatarSettings {
  const AvatarSettings({this.colorValue = 0xff2563eb, this.imagePath});

  factory AvatarSettings.fromJson(Map<String, Object?> json) {
    return AvatarSettings(
      colorValue: switch (json['avatarColor']) {
        final int value => value,
        final num value => value.toInt(),
        final String value => int.tryParse(value) ?? 0xff2563eb,
        _ => 0xff2563eb,
      },
      imagePath: json['avatarImagePath']?.toString(),
    );
  }

  final int colorValue;
  final String? imagePath;

  Map<String, Object?> toJson() {
    return {'avatarColor': colorValue, 'avatarImagePath': imagePath};
  }
}

class LocalProfileSettingsStore {
  Future<AvatarSettings> readAvatarSettings() async {
    final file = await _settingsFile();
    if (!await file.exists()) {
      return const AvatarSettings();
    }
    try {
      final payload = jsonDecode(await file.readAsString());
      if (payload is Map<String, Object?>) {
        return AvatarSettings.fromJson(payload);
      }
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
    await file.writeAsString(jsonEncode(settings.toJson()), flush: true);
  }

  Future<File> _settingsFile() async {
    final root = await getApplicationSupportDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'profile'));
    await directory.create(recursive: true);
    return File(p.join(directory.path, 'profile_settings_v1.json'));
  }
}

class AvatarSettingsController extends AsyncNotifier<AvatarSettings> {
  @override
  Future<AvatarSettings> build() {
    return ref.read(localProfileSettingsStoreProvider).readAvatarSettings();
  }

  Future<void> saveColor(int colorValue) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(localProfileSettingsStoreProvider)
          .saveAvatarColor(colorValue),
    );
  }

  Future<void> saveImage({
    required String sourcePath,
    required int fallbackColorValue,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(localProfileSettingsStoreProvider)
          .saveAvatarImage(
            sourcePath: sourcePath,
            fallbackColorValue: fallbackColorValue,
          ),
    );
  }
}
