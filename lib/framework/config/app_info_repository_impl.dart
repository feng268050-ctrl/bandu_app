import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_repositories.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final environmentAppInfoRepositoryProvider = Provider<AppInfoRepository>((ref) {
  return const EnvironmentAppInfoRepository();
});

class EnvironmentAppInfoRepository implements AppInfoRepository {
  const EnvironmentAppInfoRepository();

  @override
  Future<String> readVersion() async {
    return const String.fromEnvironment(
      'APP_VERSION',
      defaultValue: 'v0.2.0',
    );
  }
}
