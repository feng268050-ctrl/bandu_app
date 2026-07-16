import 'package:bandu_wrong_notebook/application/features/settings/domain/app_preferences.dart';
import 'package:bandu_wrong_notebook/application/features/settings/settings_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AppSettingsPage extends ConsumerWidget {
  const AppSettingsPage({required this.onBack, super.key});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final preferences =
        ref.watch(appPreferencesControllerProvider).valueOrNull ??
            const AppPreferences();
    final controller = ref.read(appPreferencesControllerProvider.notifier);
    final platformIsDark =
        MediaQuery.platformBrightnessOf(context) == Brightness.dark;
    final darkModeEnabled = switch (preferences.themePreference) {
      AppThemePreference.system => platformIsDark,
      AppThemePreference.light => false,
      AppThemePreference.dark => true,
    };

    return Scaffold(
      appBar: AppBar(
        title: const Text('设置'),
        leading: BackButton(onPressed: onBack),
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            secondary: const Icon(Icons.dark_mode_outlined),
            title: const Text('深色模式'),
            subtitle: Text(
              preferences.themePreference == AppThemePreference.system
                  ? '当前跟随系统'
                  : darkModeEnabled
                      ? '已开启'
                      : '已关闭',
            ),
            value: darkModeEnabled,
            onChanged: (enabled) => _save(
              context,
              () => controller.setThemePreference(
                enabled ? AppThemePreference.dark : AppThemePreference.light,
              ),
            ),
          ),
          const Divider(),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.format_size),
            title: const Text('字体大小'),
            subtitle: Text(preferences.fontScale.label),
          ),
          Slider(
            value: preferences.fontScale.index.toDouble(),
            min: 0,
            max: (AppFontScale.values.length - 1).toDouble(),
            divisions: AppFontScale.values.length - 1,
            label: preferences.fontScale.label,
            semanticFormatterCallback: (_) => preferences.fontScale.label,
            onChanged: (value) {
              final selected = AppFontScale.values[value.round()];
              if (selected == preferences.fontScale) return;
              _save(context, () => controller.setFontScale(selected));
            },
          ),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: AppSpacing.small),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('小'),
                Text('标准'),
                Text('大'),
                Text('特大'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _save(
    BuildContext context,
    Future<void> Function() operation,
  ) async {
    try {
      await operation();
    } catch (_) {
      if (context.mounted) {
        showAppErrorSnackBar(context, '设置保存失败，请重试');
      }
    }
  }
}

extension on AppFontScale {
  String get label => switch (this) {
        AppFontScale.small => '小',
        AppFontScale.standard => '标准',
        AppFontScale.large => '大',
        AppFontScale.extraLarge => '特大',
      };
}
