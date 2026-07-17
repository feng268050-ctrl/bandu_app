import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/avatar_settings_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('profile visuals keep avatar and wallpaper paths', () {
    const mapper = AvatarSettingsMapper();
    const settings = AvatarSettings(
      colorValue: 0xff123456,
      imagePath: '/data/avatar.jpg',
      wallpaperPath: '/data/wallpaper.jpg',
    );

    final record = mapper.toRecord(settings);
    final restored = mapper.fromRecord(
      AvatarSettingsRecord.fromJson(record.toJson()),
    );

    expect(restored.colorValue, settings.colorValue);
    expect(restored.imagePath, settings.imagePath);
    expect(restored.wallpaperPath, settings.wallpaperPath);
  });
}
