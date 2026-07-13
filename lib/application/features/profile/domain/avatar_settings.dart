class AvatarSettings {
  const AvatarSettings({this.colorValue = 0xff2563eb, this.imagePath});

  final int colorValue;
  final String? imagePath;
}

enum AvatarImageSource { camera, gallery }
