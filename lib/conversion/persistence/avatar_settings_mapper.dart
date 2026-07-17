import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class AvatarSettingsRecord {
  const AvatarSettingsRecord({
    required this.colorValue,
    this.imagePath,
    this.wallpaperPath,
  });

  factory AvatarSettingsRecord.fromJson(JsonObject json) {
    return AvatarSettingsRecord(
      colorValue: nullableIntValue(json['avatarColor']) ?? 0xff2563eb,
      imagePath: json['avatarImagePath']?.toString(),
      wallpaperPath: json['wallpaperPath']?.toString(),
    );
  }

  final int colorValue;
  final String? imagePath;
  final String? wallpaperPath;

  JsonObject toJson() => {
        'avatarColor': colorValue,
        'avatarImagePath': imagePath,
        'wallpaperPath': wallpaperPath,
      };
}

class AvatarSettingsMapper {
  const AvatarSettingsMapper();

  AvatarSettings fromRecord(AvatarSettingsRecord record) {
    return AvatarSettings(
      colorValue: record.colorValue,
      imagePath: record.imagePath,
      wallpaperPath: record.wallpaperPath,
    );
  }

  AvatarSettingsRecord toRecord(AvatarSettings settings) {
    return AvatarSettingsRecord(
      colorValue: settings.colorValue,
      imagePath: settings.imagePath,
      wallpaperPath: settings.wallpaperPath,
    );
  }
}
