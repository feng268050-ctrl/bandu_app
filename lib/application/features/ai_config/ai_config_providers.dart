import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/application/ai_config_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final aiConfigRepositoryProvider = Provider<AiConfigRepository>(
  (ref) => missingDependency('AiConfigRepository'),
);

final loadAiConfigsUseCaseProvider = Provider<LoadAiConfigsUseCase>((ref) {
  return LoadAiConfigsUseCase(ref.watch(aiConfigRepositoryProvider));
});

final saveAiConfigUseCaseProvider = Provider<SaveAiConfigUseCase>((ref) {
  return SaveAiConfigUseCase(ref.watch(aiConfigRepositoryProvider));
});

final setDefaultAiConfigUseCaseProvider =
    Provider<SetDefaultAiConfigUseCase>((ref) {
  return SetDefaultAiConfigUseCase(ref.watch(aiConfigRepositoryProvider));
});

final deleteAiConfigUseCaseProvider = Provider<DeleteAiConfigUseCase>((ref) {
  return DeleteAiConfigUseCase(ref.watch(aiConfigRepositoryProvider));
});
