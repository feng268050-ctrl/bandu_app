import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/app_preferences_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('app preferences survive a typed JSON round trip', () {
    const mapper = AppPreferencesMapper();
    const preferences = AppPreferences(
      themePreference: AppThemePreference.dark,
      fontScale: AppFontScale.large,
    );

    final record = mapper.toRecord(preferences);
    final restored = mapper.fromRecord(
      AppPreferencesRecord.fromJson(record.toJson()),
    );

    expect(restored.themePreference, AppThemePreference.dark);
    expect(restored.fontScale, AppFontScale.large);
  });

  test('unknown persisted values fall back to system defaults', () {
    const mapper = AppPreferencesMapper();
    final restored = mapper.fromRecord(
      const AppPreferencesRecord(
        themePreference: 'unknown',
        fontScale: 'huge',
      ),
    );

    expect(restored.themePreference, AppThemePreference.system);
    expect(restored.fontScale, AppFontScale.standard);
  });
}
