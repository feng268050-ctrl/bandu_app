import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/profile/application/profile_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final avatarRepositoryProvider = Provider<AvatarRepository>(
  (ref) => missingDependency('AvatarRepository'),
);

final deviceInfoRepositoryProvider = Provider<DeviceInfoRepository>(
  (ref) => missingDependency('DeviceInfoRepository'),
);

final localDataRepositoryProvider = Provider<LocalDataRepository>(
  (ref) => missingDependency('LocalDataRepository'),
);

final appInfoRepositoryProvider = Provider<AppInfoRepository>(
  (ref) => missingDependency('AppInfoRepository'),
);

final deviceNameProvider = FutureProvider<String>((ref) {
  return ref.watch(deviceInfoRepositoryProvider).readDeviceName();
});

final packageVersionProvider = FutureProvider<String>((ref) {
  return ref.watch(appInfoRepositoryProvider).readVersion();
});

final clearLocalDataUseCaseProvider = Provider<ClearLocalDataUseCase>((ref) {
  return ClearLocalDataUseCase(ref.watch(localDataRepositoryProvider));
});

final avatarSettingsProvider =
    AsyncNotifierProvider<AvatarSettingsController, AvatarSettings>(
  AvatarSettingsController.new,
);

class AvatarSettingsController extends AsyncNotifier<AvatarSettings> {
  @override
  Future<AvatarSettings> build() {
    return ref.read(avatarRepositoryProvider).readSettings();
  }

  Future<String?> pickImage(AvatarImageSource source) {
    return ref.read(avatarRepositoryProvider).pickImage(source);
  }

  Future<void> saveColor(int colorValue) async {
    await _save(
      () => ref.read(avatarRepositoryProvider).saveColor(colorValue),
    );
  }

  Future<void> saveImage({
    required String sourcePath,
    required int fallbackColorValue,
  }) async {
    await _save(
      () => ref.read(avatarRepositoryProvider).saveImage(
            sourcePath: sourcePath,
            fallbackColorValue: fallbackColorValue,
          ),
    );
  }

  Future<String?> pickWallpaper() {
    return ref.read(avatarRepositoryProvider).pickWallpaper();
  }

  Future<void> saveWallpaper(String sourcePath) async {
    await _save(
      () => ref.read(avatarRepositoryProvider).saveWallpaper(sourcePath),
    );
  }

  Future<void> removeWallpaper() async {
    await _save(
      () => ref.read(avatarRepositoryProvider).removeWallpaper(),
    );
  }

  Future<void> _save(Future<AvatarSettings> Function() operation) async {
    state = const AsyncLoading();
    try {
      state = AsyncData(await operation());
    } catch (error, stackTrace) {
      state = AsyncError(error, stackTrace);
      Error.throwWithStackTrace(error, stackTrace);
    }
  }
}
