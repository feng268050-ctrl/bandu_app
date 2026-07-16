import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';

abstract final class AppSizes {
  static const double minTouchTarget = 48;
  static const double iconSmall = 18;
  static const double iconMedium = 24;
  static const double bottomNavigationIcon = 26;
  static const double bottomNavigationHeight = 72;
  static const double primaryNavigationAction = 72;
  static const double primaryNavigationCradle = 88;
  static const double primaryNavigationLabelAlignmentOffset =
      (bottomNavigationHeight +
              bottomNavigationIcon +
              AppSpacing.xSmall -
              primaryNavigationAction) /
          2;
  static const double primaryNavigationBodyOverlap =
      primaryNavigationCradle / 2 - primaryNavigationLabelAlignmentOffset;
  static const double avatarSmall = 32;
  static const double avatarMedium = 48;
  static const double avatarLarge = 72;
}
