import 'package:bandu_wrong_notebook/application/app/app_router.dart';
import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/application/features/settings/settings_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class BanduApp extends ConsumerWidget {
  const BanduApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    final preferences =
        ref.watch(appPreferencesControllerProvider).valueOrNull ??
            const AppPreferences();

    return MaterialApp.router(
      debugShowCheckedModeBanner: false,
      title: '伴读题集',
      theme: buildAppTheme(Brightness.light),
      darkTheme: buildAppTheme(Brightness.dark),
      themeMode: switch (preferences.themePreference) {
        AppThemePreference.system => ThemeMode.system,
        AppThemePreference.light => ThemeMode.light,
        AppThemePreference.dark => ThemeMode.dark,
      },
      builder: (context, child) {
        final mediaQuery = MediaQuery.of(context);
        final systemScale = mediaQuery.textScaler.scale(1);
        final effectiveScale = (systemScale * preferences.fontScale.factor)
            .clamp(0.8, 1.6)
            .toDouble();
        return MediaQuery(
          data: mediaQuery.copyWith(
            textScaler: TextScaler.linear(effectiveScale),
          ),
          child: child ?? const SizedBox.shrink(),
        );
      },
      routerConfig: router,
    );
  }
}
