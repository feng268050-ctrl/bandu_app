import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_repository.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:bandu_wrong_notebook/framework/persistence/settings/local_app_preferences.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/question_bank_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/tutor_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/question_bank/local_question_bank_store.dart';
import 'package:bandu_wrong_notebook/framework/persistence/tutor/local_tutor_session_store.dart';
import 'package:bandu_wrong_notebook/framework/sync/pending_task_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final frameworkLocalDataRepositoryProvider = Provider<LocalDataRepository>((
  ref,
) {
  return FrameworkLocalDataRepository(
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    profileSettingsStore: ref.watch(localProfileSettingsStoreProvider),
    appPreferencesStore: ref.watch(localAppPreferencesStoreProvider),
    questionBankStore: ref.watch(localQuestionBankStoreProvider),
    tutorSessionStore: ref.watch(localTutorSessionStoreProvider),
    pendingTaskRepository: ref.watch(frameworkPendingTaskRepositoryProvider),
  );
});

class FrameworkLocalDataRepository implements LocalDataRepository {
  const FrameworkLocalDataRepository({
    required this.cacheDatabase,
    required this.profileSettingsStore,
    required this.appPreferencesStore,
    required this.questionBankStore,
    required this.tutorSessionStore,
    required this.pendingTaskRepository,
  });

  final AppCacheDatabase cacheDatabase;
  final LocalProfileSettingsStore profileSettingsStore;
  final LocalAppPreferencesStore appPreferencesStore;
  final LocalQuestionBankStore questionBankStore;
  final LocalTutorSessionStore tutorSessionStore;
  final PendingTaskRepository pendingTaskRepository;

  @override
  Future<void> clear() async {
    await cacheDatabase.clear();
    await profileSettingsStore.clear();
    await appPreferencesStore.clear();
    await questionBankStore.clear();
    await tutorSessionStore.clear();
    await pendingTaskRepository.clear();
  }
}
