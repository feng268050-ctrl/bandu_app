import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/application/pending_task_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final pendingTaskRepositoryProvider = Provider<PendingTaskRepository>(
  (ref) => missingDependency('PendingTaskRepository'),
);

final watchPendingTasksUseCaseProvider = Provider<WatchPendingTasksUseCase>(
  (ref) => WatchPendingTasksUseCase(ref.watch(pendingTaskRepositoryProvider)),
);

final queueAnalyzedCaptureUseCaseProvider =
    Provider<QueueAnalyzedCaptureUseCase>(
  (ref) =>
      QueueAnalyzedCaptureUseCase(ref.watch(pendingTaskRepositoryProvider)),
);

final retryPendingTaskUseCaseProvider = Provider<RetryPendingTaskUseCase>(
  (ref) => RetryPendingTaskUseCase(ref.watch(pendingTaskRepositoryProvider)),
);

final retryAllPendingTasksUseCaseProvider =
    Provider<RetryAllPendingTasksUseCase>(
  (ref) => RetryAllPendingTasksUseCase(
    ref.watch(pendingTaskRepositoryProvider),
  ),
);

final cancelPendingTaskUseCaseProvider = Provider<CancelPendingTaskUseCase>(
  (ref) => CancelPendingTaskUseCase(ref.watch(pendingTaskRepositoryProvider)),
);

final deletePendingTaskUseCaseProvider = Provider<DeletePendingTaskUseCase>(
  (ref) => DeletePendingTaskUseCase(ref.watch(pendingTaskRepositoryProvider)),
);

final pendingTasksProvider = StreamProvider<List<PendingTask>>((ref) {
  return ref.watch(watchPendingTasksUseCaseProvider).call();
});
