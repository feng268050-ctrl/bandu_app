import 'dart:io';

import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('wallpaper persists independently from the avatar', () async {
    final root = await Directory.systemTemp.createTemp('bandu-profile-');
    addTearDown(() => root.delete(recursive: true));
    final support = Directory('${root.path}/support');
    final documents = Directory('${root.path}/documents');
    final avatarSource = File('${root.path}/avatar.jpg');
    final wallpaperSource = File('${root.path}/wallpaper.jpg');
    await avatarSource.writeAsBytes([1, 2, 3]);
    await wallpaperSource.writeAsBytes([4, 5, 6]);
    final store = LocalProfileSettingsStore(
      supportDirectory: () async => support,
      documentsDirectory: () async => documents,
    );

    final avatar = await store.saveAvatarImage(
      sourcePath: avatarSource.path,
      fallbackColorValue: 0xff123456,
    );
    final withWallpaper = await store.saveWallpaper(wallpaperSource.path);

    expect(withWallpaper.imagePath, avatar.imagePath);
    expect(await File(withWallpaper.wallpaperPath!).exists(), isTrue);

    final restored = await LocalProfileSettingsStore(
      supportDirectory: () async => support,
      documentsDirectory: () async => documents,
    ).readAvatarSettings();
    expect(restored.imagePath, isNotNull);
    expect(restored.wallpaperPath, isNotNull);

    final withoutWallpaper = await store.removeWallpaper();
    expect(withoutWallpaper.imagePath, restored.imagePath);
    expect(withoutWallpaper.wallpaperPath, isNull);

    await store.clear();
    expect(await store.readAvatarSettings(), isNotNull);
    expect(await documents.exists(), isTrue);
    expect(
      await Directory('${documents.path}/bandu/profile').exists(),
      isFalse,
    );
  });
}
