import 'dart:async';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_repository.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_repository.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/pending_task_cache_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/repositories/capture_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/persistence/files/capture_file_store.dart';
import 'package:bandu_wrong_notebook/framework/sync/pending_task_store.dart';
import 'package:bandu_wrong_notebook/framework/sync/request_id_generator.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final frameworkPendingTaskRepositoryProvider =
    Provider<PendingTaskRepository>((ref) {
  final repository = PersistentPendingTaskRepository(
    store: ref.watch(pendingTaskStoreProvider),
    captureRepository: ref.watch(backgroundCaptureRepositoryProvider),
    captureFileStore: ref.watch(captureFileStoreProvider),
  );
  ref.onDispose(repository.dispose);
  return repository;
});

class PersistentPendingTaskRepository implements PendingTaskRepository {
  PersistentPendingTaskRepository({
    required this.store,
    required this.captureRepository,
    required this.captureFileStore,
    PendingTaskCacheMapper mapper = const PendingTaskCacheMapper(),
    RequestIdGenerator? requestIdGenerator,
    DateTime Function()? clock,
  })  : _mapper = mapper,
        _requestIdGenerator = requestIdGenerator ?? RequestIdGenerator(),
        _clock = clock ?? DateTime.now;

  static const _maxTasks = 100;

  final PendingTaskStore store;
  final CaptureRepository captureRepository;
  final CaptureFileStore captureFileStore;
  final PendingTaskCacheMapper _mapper;
  final RequestIdGenerator _requestIdGenerator;
  final DateTime Function() _clock;
  final Map<String, PendingTask> _tasks = {};
  final StreamController<List<PendingTask>> _controller =
      StreamController<List<PendingTask>>.broadcast();
  Future<void>? _loadFuture;
  Future<void> _operationTail = Future<void>.value();
  Future<PendingTaskRetrySummary>? _retryAllFuture;
  final Map<String, Future<PendingTaskRunResult>> _activeRetries = {};

  @override
  Stream<List<PendingTask>> watchTasks() {
    Future<void>.microtask(() async {
      await _ensureLoaded();
      _emit();
    });
    return _controller.stream;
  }

  @override
  Future<List<PendingTask>> readTasks() async {
    await _ensureLoaded();
    await _operationTail;
    return _sortedTasks();
  }

  @override
  Future<PendingTask> enqueueAnalyzedCapture({
    required String localImagePath,
    required AnalyzeResult result,
  }) {
    return _mutate(() async {
      await _ensureLoaded();
      if (_tasks.length >= _maxTasks) {
        throw const AppFailure(
          code: 'PENDING_TASK_LIMIT_REACHED',
          message: '待上传任务已达到上限，请先重试或删除已有任务。',
        );
      }
      final now = _clock();
      final task = PendingTask(
        id: _requestIdGenerator.next(),
        kind: PendingTaskKind.saveAnalyzedCapture,
        localImagePath: localImagePath,
        result: result,
        createdAt: now,
        updatedAt: now,
      );
      _tasks[task.id] = task;
      await _persistAndEmit();
      return task;
    });
  }

  @override
  Future<PendingTaskRunResult> retryTask(String taskId) {
    final active = _activeRetries[taskId];
    if (active != null) {
      return active;
    }
    final retry = _runTask(taskId);
    _activeRetries[taskId] = retry;
    return retry.whenComplete(() {
      if (identical(_activeRetries[taskId], retry)) {
        _activeRetries.remove(taskId);
      }
    });
  }

  Future<PendingTaskRunResult> _runTask(String taskId) async {
    final preparation = await _prepareRetry(taskId);
    final task = preparation.$1;
    if (task == null) {
      return PendingTaskRunResult(
        taskId: taskId,
        outcome: PendingTaskRunOutcome.skipped,
        message: preparation.$2,
      );
    }

    if (!await captureFileStore.captureExists(task.localImagePath)) {
      const message = '本地题目图片不存在，请删除该任务后重新拍题。';
      await _markFailed(task.id, message, permanent: true);
      return PendingTaskRunResult(
        taskId: task.id,
        outcome: PendingTaskRunOutcome.failed,
        message: message,
      );
    }

    try {
      final saved = await captureRepository.saveAnalysis(
        localImagePath: task.localImagePath,
        result: task.result,
        requestId: task.id,
      );
      await _markCompleted(task.id);
      await _finalizeCompleted(task);
      return PendingTaskRunResult(
        taskId: task.id,
        outcome: PendingTaskRunOutcome.succeeded,
        message: '错题已上传并保存',
        savedErrorItemId: saved.id,
      );
    } catch (error) {
      if (error is AppFailure && error.isCancellation) {
        await _markPending(task.id, message: '上传已取消，等待下次重试。');
        return PendingTaskRunResult(
          taskId: task.id,
          outcome: PendingTaskRunOutcome.cancelled,
          message: '上传已取消，任务仍保留在本机。',
        );
      }
      final message = appFailureUserMessage(error);
      await _markFailed(task.id, message);
      return PendingTaskRunResult(
        taskId: task.id,
        outcome: PendingTaskRunOutcome.failed,
        message: message,
      );
    }
  }

  @override
  Future<PendingTaskRetrySummary> retryAll() {
    final active = _retryAllFuture;
    if (active != null) {
      return active;
    }
    final retry = _performRetryAll();
    _retryAllFuture = retry;
    return retry.whenComplete(() {
      if (identical(_retryAllFuture, retry)) {
        _retryAllFuture = null;
      }
    });
  }

  @override
  void cancelTask(String taskId) {
    final task = _tasks[taskId];
    if (task?.status == PendingTaskStatus.running) {
      captureRepository.cancelOngoingRequest();
    }
  }

  @override
  Future<void> deleteTask(String taskId) async {
    final removed = await _mutate(() async {
      await _ensureLoaded();
      final task = _tasks[taskId];
      if (task == null) {
        return null;
      }
      if (task.status == PendingTaskStatus.running) {
        throw const AppFailure(
          code: 'PENDING_TASK_RUNNING',
          message: '任务正在上传，请等待完成后再删除。',
        );
      }
      _tasks.remove(taskId);
      await _persistAndEmit();
      return task;
    });
    if (removed != null) {
      await captureFileStore.deleteCapture(removed.localImagePath);
    }
  }

  @override
  Future<void> clear() async {
    captureRepository.cancelOngoingRequest();
    await _mutate(() async {
      await _ensureLoaded();
      _tasks.clear();
      await store.clear();
      _emit();
    });
    await captureFileStore.clearAllCaptures();
  }

  void dispose() {
    _controller.close();
  }

  Future<void> _load() async {
    final records = await store.read();
    var changed = false;
    final completed = <PendingTask>[];
    for (final record in records.take(_maxTasks)) {
      var task = _mapper.fromRecord(record);
      if (task == null) {
        changed = true;
        continue;
      }
      if (task.status == PendingTaskStatus.running) {
        task = task.markPending(
          _clock(),
          message: '上次上传被中断，等待重新上传。',
        );
        changed = true;
      }
      _tasks[task.id] = task;
      if (task.status == PendingTaskStatus.completed) {
        completed.add(task);
      }
    }
    if (records.length > _maxTasks) {
      changed = true;
    }
    for (final task in completed) {
      try {
        await captureFileStore.deleteCapture(task.localImagePath);
        _tasks.remove(task.id);
        changed = true;
      } catch (_) {
        // Leave completed cleanup records in place for the next launch.
      }
    }
    if (changed) {
      await _persistAndEmit();
    }
  }

  Future<(PendingTask?, String)> _prepareRetry(String taskId) {
    return _mutate(() async {
      await _ensureLoaded();
      final task = _tasks[taskId];
      if (task == null) {
        return (null, '待上传任务不存在或已经完成。');
      }
      if (task.status == PendingTaskStatus.running) {
        return (null, '任务正在上传，请稍候。');
      }
      if (!task.canRetry) {
        return (null, '任务已达到最大重试次数，请删除后重新拍题。');
      }
      final running = task.markRunning(_clock());
      _tasks[taskId] = running;
      await _persistAndEmit();
      return (running, '');
    });
  }

  Future<PendingTaskRetrySummary> _performRetryAll() async {
    final tasks = await readTasks();
    final ids = tasks.where((task) => task.canRetry).map((task) => task.id);
    var attempted = 0;
    var succeeded = 0;
    var failed = 0;
    var cancelled = 0;
    var skipped = 0;

    for (final id in ids) {
      final result = await retryTask(id);
      switch (result.outcome) {
        case PendingTaskRunOutcome.succeeded:
          attempted += 1;
          succeeded += 1;
          break;
        case PendingTaskRunOutcome.failed:
          attempted += 1;
          failed += 1;
          break;
        case PendingTaskRunOutcome.cancelled:
          attempted += 1;
          cancelled += 1;
          break;
        case PendingTaskRunOutcome.skipped:
          skipped += 1;
          break;
      }
    }
    return PendingTaskRetrySummary(
      attempted: attempted,
      succeeded: succeeded,
      failed: failed,
      cancelled: cancelled,
      skipped: skipped,
    );
  }

  Future<void> _markPending(String taskId, {required String message}) {
    return _mutate(() async {
      final task = _tasks[taskId];
      if (task == null) {
        return;
      }
      _tasks[taskId] = task.markPending(_clock(), message: message);
      await _persistAndEmit();
    });
  }

  Future<void> _markFailed(
    String taskId,
    String message, {
    bool permanent = false,
  }) {
    return _mutate(() async {
      final task = _tasks[taskId];
      if (task == null) {
        return;
      }
      _tasks[taskId] = task.markFailed(
        _clock(),
        message,
        permanent: permanent,
      );
      await _persistAndEmit();
    });
  }

  Future<void> _markCompleted(String taskId) {
    return _mutate(() async {
      final task = _tasks[taskId];
      if (task == null) {
        return;
      }
      _tasks[taskId] = task.markCompleted(_clock());
      await _persistAndEmit();
    });
  }

  Future<void> _finalizeCompleted(PendingTask task) async {
    try {
      await captureFileStore.deleteCapture(task.localImagePath);
      await _mutate(() async {
        final current = _tasks[task.id];
        if (current?.status != PendingTaskStatus.completed) {
          return;
        }
        _tasks.remove(task.id);
        await _persistAndEmit();
      });
    } catch (_) {
      // Keep the completed task so cleanup can be retried on the next launch.
    }
  }

  Future<void> _persistAndEmit() async {
    await store.write(
      _sortedTasks().map(_mapper.toRecord).toList(growable: false),
    );
    _emit();
  }

  Future<void> _ensureLoaded() {
    return _loadFuture ??= _load();
  }

  Future<T> _mutate<T>(Future<T> Function() operation) {
    final completer = Completer<T>();
    _operationTail = _operationTail.then((_) async {
      try {
        completer.complete(await operation());
      } catch (error, stackTrace) {
        completer.completeError(error, stackTrace);
      }
    });
    return completer.future;
  }

  List<PendingTask> _sortedTasks() {
    final tasks = _tasks.values.toList();
    tasks.sort((a, b) => b.createdAt.compareTo(a.createdAt));
    return List.unmodifiable(tasks);
  }

  void _emit() {
    if (!_controller.isClosed) {
      _controller.add(_sortedTasks());
    }
  }
}
