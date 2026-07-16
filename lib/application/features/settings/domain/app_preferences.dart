enum AppThemePreference { system, light, dark }

enum AppFontScale {
  small(0.9),
  standard(1),
  large(1.15),
  extraLarge(1.3);

  const AppFontScale(this.factor);

  final double factor;
}

class AppPreferences {
  const AppPreferences({
    this.themePreference = AppThemePreference.system,
    this.fontScale = AppFontScale.standard,
  });

  final AppThemePreference themePreference;
  final AppFontScale fontScale;

  AppPreferences copyWith({
    AppThemePreference? themePreference,
    AppFontScale? fontScale,
  }) {
    return AppPreferences(
      themePreference: themePreference ?? this.themePreference,
      fontScale: fontScale ?? this.fontScale,
    );
  }
}
