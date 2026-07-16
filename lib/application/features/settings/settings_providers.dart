import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final appPreferencesRepositoryProvider = Provider<AppPreferencesRepository>(
  (ref) => missingDependency('AppPreferencesRepository'),
);

final appPreferencesControllerProvider =
    AsyncNotifierProvider<AppPreferencesController, AppPreferences>(
  AppPreferencesController.new,
);

class AppPreferencesController extends AsyncNotifier<AppPreferences> {
  @override
  Future<AppPreferences> build() {
    return ref.read(appPreferencesRepositoryProvider).read();
  }

  Future<void> setThemePreference(AppThemePreference preference) {
    return _save(_current.copyWith(themePreference: preference));
  }

  Future<void> setFontScale(AppFontScale fontScale) {
    return _save(_current.copyWith(fontScale: fontScale));
  }

  AppPreferences get _current => state.valueOrNull ?? const AppPreferences();

  Future<void> _save(AppPreferences next) async {
    final previous = _current;
    state = AsyncData(next);
    try {
      await ref.read(appPreferencesRepositoryProvider).save(next);
    } catch (error, stackTrace) {
      state = AsyncData(previous);
      Error.throwWithStackTrace(error, stackTrace);
    }
  }
}
