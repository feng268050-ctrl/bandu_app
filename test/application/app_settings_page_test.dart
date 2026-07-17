import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences_repository.dart';
import 'package:bandu_wrong_notebook/application/features/settings/presentation/app_settings_page.dart';
import 'package:bandu_wrong_notebook/application/features/settings/settings_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('settings update dark mode and global font preference', (
    tester,
  ) async {
    final repository = _FakeAppPreferencesRepository();
    var openedAiConfig = false;
    var openedDevice = false;
    var openedData = false;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          appPreferencesRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.light),
          home: AppSettingsPage(
            onBack: () {},
            onOpenAiConfig: () => openedAiConfig = true,
            onOpenDevice: () => openedDevice = true,
            onOpenData: () => openedData = true,
            modelConfigLabel: '已配置 2 个模型',
            deviceNameLabel: 'OPPO PKT110',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('深色模式'), findsOneWidget);
    expect(find.text('字体大小'), findsOneWidget);
    expect(find.text('AI 配置'), findsOneWidget);
    expect(find.text('设备名称'), findsOneWidget);
    expect(find.text('数据管理'), findsOneWidget);

    await tester.tap(find.byType(Switch));
    await tester.pumpAndSettle();
    expect(repository.value.themePreference, AppThemePreference.dark);

    final slider = tester.widget<Slider>(find.byType(Slider));
    slider.onChanged?.call(AppFontScale.extraLarge.index.toDouble());
    await tester.pumpAndSettle();
    expect(repository.value.fontScale, AppFontScale.extraLarge);
    expect(find.text('特大'), findsWidgets);

    await tester.tap(find.text('AI 配置'));
    await tester.tap(find.text('设备名称'));
    await tester.tap(find.text('数据管理'));
    expect(openedAiConfig, isTrue);
    expect(openedDevice, isTrue);
    expect(openedData, isTrue);
  });
}

class _FakeAppPreferencesRepository implements AppPreferencesRepository {
  AppPreferences value = const AppPreferences();

  @override
  Future<AppPreferences> read() async => value;

  @override
  Future<void> save(AppPreferences preferences) async {
    value = preferences;
  }
}
