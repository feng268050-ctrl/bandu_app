import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final practiceRepositoryProvider = Provider<PracticeRepository>(
  (ref) => missingDependency('PracticeRepository'),
);

final practiceHistoryProvider = FutureProvider<List<PracticeRecord>>((ref) {
  return ref.watch(practiceRepositoryProvider).history();
});
