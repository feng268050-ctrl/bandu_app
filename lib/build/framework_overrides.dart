import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/capture/capture_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/network_diagnostics_providers.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/pending_task_providers.dart';
import 'package:bandu_wrong_notebook/application/features/practice/practice_providers.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:bandu_wrong_notebook/application/features/settings/settings_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/tutor_providers.dart';
import 'package:bandu_wrong_notebook/framework/config/app_info_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:bandu_wrong_notebook/framework/device/device_info_service.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/auth_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/ai_config_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/capture_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/error_item_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/network_diagnostics_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/practice_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/question_bank_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/stats_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/tutor_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/local_data_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:bandu_wrong_notebook/framework/persistence/settings/local_app_preferences.dart';
import 'package:bandu_wrong_notebook/framework/speech/speech_input_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/sync/pending_task_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

List<Override> buildFrameworkOverrides() {
  return [
    authBypassProvider.overrideWith(
      (ref) => ref.watch(appConfigProvider).bypassAuth,
    ),
    authRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteAuthRepositoryProvider),
    ),
    aiConfigRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteAiConfigRepositoryProvider),
    ),
    captureRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteCaptureRepositoryProvider),
    ),
    errorItemRepositoryProvider.overrideWith(
      (ref) => ref.watch(remoteErrorItemRepositoryProvider),
    ),
    networkDiagnosticsRepositoryProvider.overrideWith(
      (ref) => ref.watch(frameworkNetworkDiagnosticsRepositoryProvider),
    ),
    pendingTaskRepositoryProvider.overrideWith(
      (ref) => ref.watch(frameworkPendingTaskRepositoryProvider),
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
    appPreferencesRepositoryProvider.overrideWith(
      (ref) => ref.watch(localAppPreferencesRepositoryProvider),
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
