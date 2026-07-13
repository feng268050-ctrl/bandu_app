import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class AvatarSettingsMapper {
  const AvatarSettingsMapper();

  AvatarSettings fromJson(Map<String, Object?> json) {
    return AvatarSettings(
      colorValue: nullableIntValue(json['avatarColor']) ?? 0xff2563eb,
      imagePath: json['avatarImagePath']?.toString(),
    );
  }

  Map<String, Object?> toJson(AvatarSettings settings) {
    return {
      'avatarColor': settings.colorValue,
      'avatarImagePath': settings.imagePath,
    };
  }
}
