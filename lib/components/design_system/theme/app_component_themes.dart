import 'package:bandu_wrong_notebook/components/design_system/tokens/app_radius.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

ThemeData buildAppComponentTheme(ColorScheme colorScheme) {
  final base = ThemeData(
    useMaterial3: true,
    brightness: colorScheme.brightness,
    colorScheme: colorScheme,
  );
  final buttonShape = RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(AppRadius.small),
  );
  final inputBorder = OutlineInputBorder(
    borderRadius: BorderRadius.circular(AppRadius.small),
    borderSide: BorderSide(color: colorScheme.outlineVariant),
  );

  return base.copyWith(
    scaffoldBackgroundColor: colorScheme.surface,
    textTheme: _zeroLetterSpacing(base.textTheme),
    appBarTheme: AppBarTheme(
      centerTitle: false,
      backgroundColor: colorScheme.surface,
      foregroundColor: colorScheme.onSurface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 1,
    ),
    navigationBarTheme: NavigationBarThemeData(
      height: 72,
      backgroundColor: colorScheme.surfaceContainer,
      indicatorColor: colorScheme.secondaryContainer,
      labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      color: colorScheme.surfaceContainerLow,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: colorScheme.surfaceContainerLowest,
      contentPadding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.large,
        vertical: AppSpacing.medium,
      ),
      border: inputBorder,
      enabledBorder: inputBorder,
      focusedBorder: inputBorder.copyWith(
        borderSide: BorderSide(color: colorScheme.primary, width: 2),
      ),
      errorBorder: inputBorder.copyWith(
        borderSide: BorderSide(color: colorScheme.error),
      ),
      focusedErrorBorder: inputBorder.copyWith(
        borderSide: BorderSide(color: colorScheme.error, width: 2),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(64, AppSizes.minTouchTarget),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xLarge),
        shape: buttonShape,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(64, AppSizes.minTouchTarget),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xLarge),
        shape: buttonShape,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        minimumSize: const Size(48, AppSizes.minTouchTarget),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.large),
        shape: buttonShape,
      ),
    ),
    iconButtonTheme: IconButtonThemeData(
      style: IconButton.styleFrom(
        minimumSize: const Size.square(AppSizes.minTouchTarget),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      minTileHeight: AppSizes.minTouchTarget,
      contentPadding: EdgeInsets.symmetric(horizontal: AppSpacing.large),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: colorScheme.surfaceContainerHigh,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.large),
      ),
    ),
    bottomSheetTheme: BottomSheetThemeData(
      backgroundColor: colorScheme.surfaceContainerLow,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.large),
        ),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: colorScheme.inverseSurface,
      contentTextStyle: base.textTheme.bodyMedium?.copyWith(
        color: colorScheme.onInverseSurface,
        letterSpacing: 0,
      ),
      actionTextColor: colorScheme.inversePrimary,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.small),
      ),
    ),
    chipTheme: base.chipTheme.copyWith(
      side: BorderSide(color: colorScheme.outlineVariant),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.small),
      ),
    ),
    progressIndicatorTheme: ProgressIndicatorThemeData(
      color: colorScheme.primary,
      linearTrackColor: colorScheme.surfaceContainerHighest,
      circularTrackColor: colorScheme.surfaceContainerHighest,
    ),
    dividerTheme: DividerThemeData(
      color: colorScheme.outlineVariant,
      thickness: 1,
      space: 1,
    ),
  );
}

TextTheme _zeroLetterSpacing(TextTheme theme) {
  TextStyle? zero(TextStyle? style) => style?.copyWith(letterSpacing: 0);

  return theme.copyWith(
    displayLarge: zero(theme.displayLarge),
    displayMedium: zero(theme.displayMedium),
    displaySmall: zero(theme.displaySmall),
    headlineLarge: zero(theme.headlineLarge),
    headlineMedium: zero(theme.headlineMedium),
    headlineSmall: zero(theme.headlineSmall),
    titleLarge: zero(theme.titleLarge),
    titleMedium: zero(theme.titleMedium),
    titleSmall: zero(theme.titleSmall),
    bodyLarge: zero(theme.bodyLarge),
    bodyMedium: zero(theme.bodyMedium),
    bodySmall: zero(theme.bodySmall),
    labelLarge: zero(theme.labelLarge),
    labelMedium: zero(theme.labelMedium),
    labelSmall: zero(theme.labelSmall),
  );
}
