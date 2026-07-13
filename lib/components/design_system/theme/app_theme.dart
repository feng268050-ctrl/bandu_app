import 'package:bandu_wrong_notebook/components/design_system/theme/app_color_scheme.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_component_themes.dart';
import 'package:flutter/material.dart';

ThemeData buildAppTheme(Brightness brightness) {
  return buildAppComponentTheme(AppColorSchemes.fromBrightness(brightness));
}
