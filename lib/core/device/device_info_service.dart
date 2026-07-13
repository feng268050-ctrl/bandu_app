import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final deviceNameProvider = FutureProvider<String>((ref) async {
  const channel = MethodChannel('com.bandu.tiji/device');
  try {
    final deviceName = await channel.invokeMethod<String>('deviceName');
    if (deviceName != null && deviceName.trim().isNotEmpty) {
      return deviceName.trim();
    }
  } on MissingPluginException {
    // Platform host is not available in unit tests or unsupported platforms.
  } on PlatformException {
    // Fall through to a stable Dart-side label.
  }

  if (Platform.isAndroid) {
    return 'Android 设备';
  }
  if (Platform.isIOS) {
    return 'iOS 设备';
  }
  return Platform.localHostname;
});
