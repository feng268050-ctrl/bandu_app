import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/capture/capture_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/practice/practice_providers.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/tutor_providers.dart';
import 'package:bandu_wrong_notebook/framework/config/app_info_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/device/device_info_service.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/auth_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/capture_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/error_item_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/practice_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/question_bank_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/stats_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/tutor_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/local_data_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:bandu_wrong_notebook/framework/speech/speech_input_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

List<Override> buildFrameworkOverrides() {
  return [
    authBypassProvider.overrideWith(
      (ref) => ref.watch(appConfigProvider).bypassAuth,
    ),
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
    questionBankRepositoryProvider.overrideWith(
      (ref) => ref.watch(frameworkQuestionBankRepositoryProvider),
    ),
    statsRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteStatsRepositoryProvider),
    ),
    tutorRepositoryProvider.overrideWith(
      (ref) => ref.watch(frameworkTutorRepositoryProvider),
    ),
    speechInputRepositoryProvider.overrideWith(
      (ref) => ref.watch(platformSpeechInputRepositoryProvider),
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
