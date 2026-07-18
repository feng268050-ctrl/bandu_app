import 'package:bandu_wrong_notebook/components/design_system/tokens/app_radius.dart';
import 'package:flutter/material.dart';

abstract final class AppShapes {
  static const BorderRadius searchFieldBorderRadius = BorderRadius.all(
    Radius.circular(AppRadius.xLarge),
  );

  static const ShapeBorder roundedListTile = RoundedRectangleBorder(
    borderRadius: BorderRadius.all(Radius.circular(AppRadius.medium)),
  );
}
