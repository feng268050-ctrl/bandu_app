import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/practice/application/practice_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceRepositoryProvider = Provider<PracticeRepository>(
  (ref) => missingDependency('PracticeRepository'),
);

final generatePracticeUseCaseProvider = Provider<GeneratePracticeUseCase>((
  ref,
) {
  return GeneratePracticeUseCase(ref.watch(practiceRepositoryProvider));
});

final recordPracticeResultUseCaseProvider =
    Provider<RecordPracticeResultUseCase>((ref) {
  return RecordPracticeResultUseCase(ref.watch(practiceRepositoryProvider));
});

final fetchPracticeHistoryUseCaseProvider =
    Provider<FetchPracticeHistoryUseCase>((ref) {
  return FetchPracticeHistoryUseCase(ref.watch(practiceRepositoryProvider));
});

final practiceHistoryProvider = FutureProvider<List<PracticeRecord>>((ref) {
  return ref.watch(fetchPracticeHistoryUseCaseProvider).call();
});
