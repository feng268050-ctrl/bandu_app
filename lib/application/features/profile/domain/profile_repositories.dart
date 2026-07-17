import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';

abstract interface class AvatarRepository {
  Future<AvatarSettings> readSettings();

  Future<String?> pickImage(AvatarImageSource source);

  Future<AvatarSettings> saveColor(int colorValue);

  Future<AvatarSettings> saveImage({
    required String sourcePath,
    required int fallbackColorValue,
  });

  Future<String?> pickWallpaper();

  Future<AvatarSettings> saveWallpaper(String sourcePath);

  Future<AvatarSettings> removeWallpaper();
}

abstract interface class DeviceInfoRepository {
  Future<String> readDeviceName();
}

abstract interface class LocalDataRepository {
  Future<void> clear();
}

abstract interface class AppInfoRepository {
  Future<String> readVersion();
}
