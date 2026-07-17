class AvatarSettings {
  const AvatarSettings({
    this.colorValue = 0xff2563eb,
    this.imagePath,
    this.wallpaperPath,
  });

  final int colorValue;
  final String? imagePath;
  final String? wallpaperPath;
}

enum AvatarImageSource { camera, gallery }
