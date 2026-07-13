import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final aiConfigControllerProvider =
    AsyncNotifierProvider<AiConfigController, List<AiServiceConfig>>(
  AiConfigController.new,
);

class AiConfigController extends AsyncNotifier<List<AiServiceConfig>> {
  @override
  Future<List<AiServiceConfig>> build() {
    return ref.read(loadAiConfigsUseCaseProvider).call();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(loadAiConfigsUseCaseProvider).call(),
    );
  }

  Future<void> save({
    required AiServiceConfigDraft draft,
    String? id,
  }) async {
    await ref.read(saveAiConfigUseCaseProvider).call(
          id: id,
          draft: draft,
        );
    await _reload();
  }

  Future<void> setDefault(String id) async {
    await ref.read(setDefaultAiConfigUseCaseProvider).call(id);
    await _reload();
  }

  Future<void> delete(String id) async {
    await ref.read(deleteAiConfigUseCaseProvider).call(id);
    await _reload();
  }

  Future<void> _reload() async {
    final configs = await ref.read(loadAiConfigsUseCaseProvider).call();
    state = AsyncData(configs);
  }
}
