import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final platformDeviceInfoRepositoryProvider = Provider<DeviceInfoRepository>((
  ref,
) {
  return const PlatformDeviceInfoRepository();
});

class PlatformDeviceInfoRepository implements DeviceInfoRepository {
  const PlatformDeviceInfoRepository();

  static const _channel = MethodChannel('com.bandu.tiji/device');

  @override
  Future<String> readDeviceName() async {
    try {
      final deviceName = await _channel.invokeMethod<String>('deviceName');
      if (deviceName != null && deviceName.trim().isNotEmpty) {
        return deviceName.trim();
      }
    } on MissingPluginException {
      // Unit tests and unsupported hosts use the stable Dart-side label.
    } on PlatformException {
      // Platform failures fall back to a readable generic name.
    }

    if (Platform.isAndroid) {
      return 'Android 设备';
    }
    if (Platform.isIOS) {
      return 'iOS 设备';
    }
    return Platform.localHostname;
  }
}
