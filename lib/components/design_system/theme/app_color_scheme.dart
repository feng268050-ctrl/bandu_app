import 'package:flutter/material.dart';

abstract final class AppColorSchemes {
  static const seedColor = Color(0xff2563eb);

  static ColorScheme fromBrightness(Brightness brightness) {
    return ColorScheme.fromSeed(seedColor: seedColor, brightness: brightness);
  }
}
