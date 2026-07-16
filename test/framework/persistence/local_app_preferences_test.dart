import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/framework/persistence/settings/local_app_preferences.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('app preferences persist across store instances', () async {
    final directory = await Directory.systemTemp.createTemp(
      'bandu-app-preferences-',
    );
    addTearDown(() => directory.delete(recursive: true));
    Future<Directory> supportDirectory() async => directory;

    final first = LocalAppPreferencesStore(
      supportDirectory: supportDirectory,
    );
    expect((await first.read()).themePreference, AppThemePreference.system);

    await first.save(
      const AppPreferences(
        themePreference: AppThemePreference.dark,
        fontScale: AppFontScale.extraLarge,
      ),
    );

    final second = LocalAppPreferencesStore(
      supportDirectory: supportDirectory,
    );
    final restored = await second.read();
    expect(restored.themePreference, AppThemePreference.dark);
    expect(restored.fontScale, AppFontScale.extraLarge);

    await second.clear();
    expect((await second.read()).themePreference, AppThemePreference.system);
  });
}
