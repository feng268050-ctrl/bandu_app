import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:bandu_wrong_notebook/framework/persistence/cache/app_cache_database.dart';
import 'package:bandu_wrong_notebook/framework/persistence/profile/local_profile_settings.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final frameworkLocalDataRepositoryProvider = Provider<LocalDataRepository>((
  ref,
) {
  return FrameworkLocalDataRepository(
    cacheDatabase: ref.watch(appCacheDatabaseProvider),
    profileSettingsStore: ref.watch(localProfileSettingsStoreProvider),
  );
});

class FrameworkLocalDataRepository implements LocalDataRepository {
  const FrameworkLocalDataRepository({
    required this.cacheDatabase,
    required this.profileSettingsStore,
  });

  final AppCacheDatabase cacheDatabase;
  final LocalProfileSettingsStore profileSettingsStore;

  @override
  Future<void> clear() async {
    await cacheDatabase.clear();
    await profileSettingsStore.clear();
  }
}
