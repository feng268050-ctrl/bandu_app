import 'dart:io';
import 'dart:math';

import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_repository.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/pending_task_cache_mapper.dart';
import 'package:bandu_wrong_notebook/framework/persistence/files/capture_file_store.dart';
import 'package:bandu_wrong_notebook/framework/sync/pending_task_repository_impl.dart';
import 'package:bandu_wrong_notebook/framework/sync/pending_task_store.dart';
import 'package:bandu_wrong_notebook/framework/sync/request_id_generator.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as p;

void main() {
  const result = AnalyzeResult(
    title: '分数加法',
    subjectName: '数学',
    questionText: '1/2 + 1/2',
    answer: '1',
    analysis: '同分母相加',
    tags: ['分数'],
  );
  final now = DateTime.utc(2026, 7, 16, 8);

  test('failed task survives repository restart', () async {
    final store = MemoryPendingTaskStore();
    final uploader = _FakeCaptureRepository(
      error: const AppFailure(code: 'NETWORK_UNAVAILABLE', message: '网络不可用'),
    );
    final files = _FakeCaptureFileStore()..existing.add('/capture/one.jpg');
    final repository = _repository(
      store: store,
      uploader: uploader,
      files: files,
      now: now,
    );

    final task = await repository.enqueueAnalyzedCapture(
      localImagePath: '/capture/one.jpg',
      result: result,
    );
    final run = await repository.retryTask(task.id);
    repository.dispose();

    expect(run.outcome, PendingTaskRunOutcome.failed);
    final restored = _repository(
      store: store,
      uploader: uploader,
      files: files,
      now: now,
    );
    final tasks = await restored.readTasks();
    expect(tasks.single.id, task.id);
    expect(tasks.single.retryCount, 1);
    expect(tasks.single.status, PendingTaskStatus.failed);
    restored.dispose();
  });

  test('successful upload uses UUID request id and removes image and task',
      () async {
    final store = MemoryPendingTaskStore();
    final uploader = _FakeCaptureRepository();
    final files = _FakeCaptureFileStore()..existing.add('/capture/two.jpg');
    final repository = _repository(
      store: store,
      uploader: uploader,
      files: files,
      now: now,
    );

    final task = await repository.enqueueAnalyzedCapture(
      localImagePath: '/capture/two.jpg',
      result: result,
    );
    final run = await repository.retryTask(task.id);

    expect(
      task.id,
      matches(
        RegExp(
          r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
        ),
      ),
    );
    expect(run.isSuccess, isTrue);
    expect(run.savedErrorItemId, 'saved-1');
    expect(uploader.requestIds, [task.id]);
    expect(files.deleted, ['/capture/two.jpg']);
    expect(await repository.readTasks(), isEmpty);
    expect(await store.read(), isEmpty);
    repository.dispose();
  });

  test('cancelled upload remains pending without consuming retry count',
      () async {
    final store = MemoryPendingTaskStore();
    final uploader = _FakeCaptureRepository(
      error: const AppFailure(code: 'REQUEST_CANCELLED', message: '已取消'),
    );
    final files = _FakeCaptureFileStore()..existing.add('/capture/three.jpg');
    final repository = _repository(
      store: store,
      uploader: uploader,
      files: files,
      now: now,
    );
    final task = await repository.enqueueAnalyzedCapture(
      localImagePath: '/capture/three.jpg',
      result: result,
    );

    final run = await repository.retryTask(task.id);
    final pending = (await repository.readTasks()).single;

    expect(run.outcome, PendingTaskRunOutcome.cancelled);
    expect(pending.status, PendingTaskStatus.pending);
    expect(pending.retryCount, 0);
    repository.dispose();
  });

  test('automatic retries stop at configured maximum', () async {
    final uploader = _FakeCaptureRepository(
      error: const AppFailure(code: 'HTTP_503', message: '服务暂不可用'),
    );
    final files = _FakeCaptureFileStore()..existing.add('/capture/four.jpg');
    final repository = _repository(
      store: MemoryPendingTaskStore(),
      uploader: uploader,
      files: files,
      now: now,
    );
    final task = await repository.enqueueAnalyzedCapture(
      localImagePath: '/capture/four.jpg',
      result: result,
    );

    for (var attempt = 0; attempt < maxPendingTaskRetries; attempt += 1) {
      expect(
        (await repository.retryTask(task.id)).outcome,
        PendingTaskRunOutcome.failed,
      );
    }
    final skipped = await repository.retryTask(task.id);

    expect(skipped.outcome, PendingTaskRunOutcome.skipped);
    expect(uploader.saveCalls, maxPendingTaskRetries);
    expect((await repository.readTasks()).single.canRetry, isFalse);
    repository.dispose();
  });

  test('running task is restored to pending after interrupted launch',
      () async {
    final task = PendingTask(
      id: '2f72c13c-ae8a-4e29-8e63-cfef3b740a80',
      kind: PendingTaskKind.saveAnalyzedCapture,
      localImagePath: '/capture/five.jpg',
      result: result,
      createdAt: now,
      updatedAt: now,
      status: PendingTaskStatus.running,
    );
    final store = MemoryPendingTaskStore([
      const PendingTaskCacheMapper().toRecord(task),
    ]);
    final repository = _repository(
      store: store,
      uploader: _FakeCaptureRepository(),
      files: _FakeCaptureFileStore()..existing.add('/capture/five.jpg'),
      now: now,
    );

    final restored = (await repository.readTasks()).single;

    expect(restored.status, PendingTaskStatus.pending);
    expect(restored.lastError, contains('中断'));
    repository.dispose();
  });

  test('file task store survives re-instantiation and rebuilds corruption',
      () async {
    final directory = await Directory.systemTemp.createTemp('bandu-sync-test-');
    addTearDown(() => directory.delete(recursive: true));
    final store = FilePendingTaskStore(supportDirectory: () async => directory);
    final record = const PendingTaskCacheMapper().toRecord(
      PendingTask(
        id: '2f72c13c-ae8a-4e29-8e63-cfef3b740a80',
        kind: PendingTaskKind.saveAnalyzedCapture,
        localImagePath: '/capture/six.jpg',
        result: result,
        createdAt: now,
        updatedAt: now,
      ),
    );
    await store.write([record]);

    final reopened = FilePendingTaskStore(
      supportDirectory: () async => directory,
    );
    expect((await reopened.read()).single.id, record.id);

    final file = File(
      p.join(directory.path, 'bandu', 'sync', 'pending_tasks_v1.json'),
    );
    await file.rename('${file.path}.tmp');
    final recovered = FilePendingTaskStore(
      supportDirectory: () async => directory,
    );
    expect((await recovered.read()).single.id, record.id);
    expect(await file.exists(), isTrue);

    await file.writeAsString('{broken');
    final corrupted = FilePendingTaskStore(
      supportDirectory: () async => directory,
    );
    expect(await corrupted.read(), isEmpty);
    expect(await file.exists(), isFalse);
  });
}

PersistentPendingTaskRepository _repository({
  required PendingTaskStore store,
  required _FakeCaptureRepository uploader,
  required _FakeCaptureFileStore files,
  required DateTime now,
}) {
  return PersistentPendingTaskRepository(
    store: store,
    captureRepository: uploader,
    captureFileStore: files,
    requestIdGenerator: RequestIdGenerator(random: Random(17)),
    clock: () => now,
  );
}

class _FakeCaptureRepository implements CaptureRepository {
  _FakeCaptureRepository({this.error});

  final Object? error;
  final List<String> requestIds = [];
  int saveCalls = 0;
  bool cancelled = false;

  @override
  Future<AnalyzeResult> analyzeImage(String localImagePath) {
    throw UnimplementedError();
  }

  @override
  void cancelOngoingRequest() {
    cancelled = true;
  }

  @override
  Future<String?> pickFromGallery() async => null;

  @override
  Future<SavedErrorItem> saveAnalysis({
    required String localImagePath,
    required AnalyzeResult result,
    required String requestId,
  }) async {
    saveCalls += 1;
    requestIds.add(requestId);
    final failure = error;
    if (failure != null) {
      throw failure;
    }
    return const SavedErrorItem(
      id: 'saved-1',
      title: '分数加法',
      subjectName: '数学',
    );
  }

  @override
  Future<String?> takePhoto() async => null;
}

class _FakeCaptureFileStore extends CaptureFileStore {
  final Set<String> existing = {};
  final List<String> deleted = [];

  @override
  Future<bool> captureExists(String filePath) async {
    return existing.contains(filePath);
  }

  @override
  Future<void> deleteCapture(String filePath) async {
    existing.remove(filePath);
    deleted.add(filePath);
  }

  @override
  Future<void> clearAllCaptures() async {
    existing.clear();
  }
}
