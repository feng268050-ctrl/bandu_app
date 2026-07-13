import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/capture/capture_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/practice/practice_providers.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/framework/config/app_info_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/device/device_info_service.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/auth_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/capture_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/error_item_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/practice_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/stats_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/local_data_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

List<Override> buildFrameworkOverrides() {
  return [
    authRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteAuthRepositoryProvider),
    ),
    captureRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteCaptureRepositoryProvider),
    ),
    errorItemRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteErrorItemRepositoryProvider),
    ),
    practiceRepositoryProvider.overrideWith(
      (ref) => ref.watch(remotePracticeRepositoryProvider),
    ),
    statsRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteStatsRepositoryProvider),
    ),
    avatarRepositoryProvider.overrideWith(
      (ref) => ref.watch(localAvatarRepositoryProvider),
    ),
    deviceInfoRepositoryProvider.overrideWith(
      (ref) => ref.watch(platformDeviceInfoRepositoryProvider),
    ),
    localDataRepositoryProvider.overrideWith(
      (ref) => ref.watch(frameworkLocalDataRepositoryProvider),
    ),
    appInfoRepositoryProvider.overrideWith(
      (ref) => ref.watch(environmentAppInfoRepositoryProvider),
    ),
  ];
}
