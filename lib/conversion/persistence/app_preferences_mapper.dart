import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';

class AppPreferencesRecord {
  const AppPreferencesRecord({
    required this.themePreference,
    required this.fontScale,
  });

  factory AppPreferencesRecord.fromJson(JsonObject json) {
    return AppPreferencesRecord(
      themePreference: json['themePreference']?.toString() ?? 'system',
      fontScale: json['fontScale']?.toString() ?? 'standard',
    );
  }

  final String themePreference;
  final String fontScale;

  JsonObject toJson() => {
        'themePreference': themePreference,
        'fontScale': fontScale,
      };
}

class AppPreferencesMapper {
  const AppPreferencesMapper();

  AppPreferences fromRecord(AppPreferencesRecord record) {
    return AppPreferences(
      themePreference: AppThemePreference.values.firstWhere(
        (value) => value.name == record.themePreference,
        orElse: () => AppThemePreference.system,
      ),
      fontScale: AppFontScale.values.firstWhere(
        (value) => value.name == record.fontScale,
        orElse: () => AppFontScale.standard,
      ),
    );
  }

  AppPreferencesRecord toRecord(AppPreferences preferences) {
    return AppPreferencesRecord(
      themePreference: preferences.themePreference.name,
      fontScale: preferences.fontScale.name,
    );
  }
}
