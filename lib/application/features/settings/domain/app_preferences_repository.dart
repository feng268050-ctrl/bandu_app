import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';

abstract interface class AppPreferencesRepository {
  Future<AppPreferences> read();

  Future<void> save(AppPreferences preferences);
}
