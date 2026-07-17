import 'package:bandu_wrong_notebook/application/features/ai_config/ai_config_providers.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final aiModelCatalogControllerProvider =
    AsyncNotifierProvider<AiModelCatalogController, AiModelCatalog>(
  AiModelCatalogController.new,
);

class AiModelCatalogController extends AsyncNotifier<AiModelCatalog> {
  @override
  Future<AiModelCatalog> build() =>
      ref.read(aiModelRepositoryProvider).fetchCatalog();

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(aiModelRepositoryProvider).fetchCatalog(),
    );
  }

  Future<void> save(AiModelDraft draft, {String? id}) async {
    final repository = ref.read(aiModelRepositoryProvider);
    if (id == null) {
      await repository.createModel(draft);
    } else {
      await repository.updateModel(id, draft);
    }
    await refresh();
  }

  Future<void> delete(AiModelSummary model) async {
    if (model.isSystem) throw StateError('系统模型不可删除');
    await ref.read(aiModelRepositoryProvider).deleteModel(model.id);
    await refresh();
  }
}

final aiPurposePreferencesControllerProvider =
    AsyncNotifierProvider<AiPurposePreferencesController, List<AiPurposePreference>>(
  AiPurposePreferencesController.new,
);

class AiPurposePreferencesController
    extends AsyncNotifier<List<AiPurposePreference>> {
  @override
  Future<List<AiPurposePreference>> build() async {
    final received = await ref.read(aiPreferenceRepositoryProvider).fetchPreferences();
    return [
      for (final purpose in AiPurpose.values)
        received.where((item) => item.purpose == purpose).firstOrNull ??
            AiPurposePreference(purpose: purpose),
    ];
  }

  Future<void> save(AiPurposePreference preference) async {
    await ref.read(aiPreferenceRepositoryProvider).savePreference(preference);
    state = AsyncData([
      for (final item in state.valueOrNull ?? const <AiPurposePreference>[])
        item.purpose == preference.purpose ? preference : item,
    ]);
  }
}
