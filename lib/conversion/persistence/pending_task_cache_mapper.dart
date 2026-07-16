import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/domain/pending_task_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class PendingTaskCacheRecord {
  const PendingTaskCacheRecord({
    required this.id,
    required this.kind,
    required this.localImagePath,
    required this.title,
    required this.tags,
    required this.createdAt,
    required this.updatedAt,
    required this.status,
    required this.retryCount,
    this.subjectName,
    this.questionText,
    this.answer,
    this.analysis,
    this.lastError,
  });

  factory PendingTaskCacheRecord.fromJson(JsonObject json) {
    return PendingTaskCacheRecord(
      id: json['id']?.toString() ?? '',
      kind: json['kind']?.toString() ?? '',
      localImagePath: json['localImagePath']?.toString() ?? '',
      title: json['title']?.toString() ?? '未命名错题',
      subjectName: json['subjectName']?.toString(),
      questionText: json['questionText']?.toString(),
      answer: json['answer']?.toString(),
      analysis: json['analysis']?.toString(),
      tags: stringListValue(json['tags']),
      createdAt: dateTimeValue(json['createdAt']),
      updatedAt: dateTimeValue(json['updatedAt']),
      status: json['status']?.toString() ?? 'pending',
      retryCount: intValue(json['retryCount']),
      lastError: json['lastError']?.toString(),
    );
  }

  final String id;
  final String kind;
  final String localImagePath;
  final String title;
  final String? subjectName;
  final String? questionText;
  final String? answer;
  final String? analysis;
  final List<String> tags;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String status;
  final int retryCount;
  final String? lastError;

  JsonObject toJson() {
    return {
      'id': id,
      'kind': kind,
      'localImagePath': localImagePath,
      'title': title,
      'subjectName': subjectName,
      'questionText': questionText,
      'answer': answer,
      'analysis': analysis,
      'tags': tags,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
      'status': status,
      'retryCount': retryCount,
      'lastError': lastError,
    };
  }
}

class PendingTaskCacheMapper {
  const PendingTaskCacheMapper();

  PendingTaskCacheRecord toRecord(PendingTask task) {
    return PendingTaskCacheRecord(
      id: task.id,
      kind: task.kind.name,
      localImagePath: task.localImagePath,
      title: task.result.title,
      subjectName: task.result.subjectName,
      questionText: task.result.questionText,
      answer: task.result.answer,
      analysis: task.result.analysis,
      tags: task.result.tags,
      createdAt: task.createdAt,
      updatedAt: task.updatedAt,
      status: task.status.name,
      retryCount: task.retryCount,
      lastError: task.lastError,
    );
  }

  PendingTask? fromRecord(PendingTaskCacheRecord record) {
    if (record.id.isEmpty ||
        record.localImagePath.isEmpty ||
        record.kind != PendingTaskKind.saveAnalyzedCapture.name) {
      return null;
    }

    return PendingTask(
      id: record.id,
      kind: PendingTaskKind.saveAnalyzedCapture,
      localImagePath: record.localImagePath,
      result: AnalyzeResult(
        title: record.title,
        subjectName: record.subjectName,
        questionText: record.questionText,
        answer: record.answer,
        analysis: record.analysis,
        tags: record.tags,
      ),
      createdAt: record.createdAt,
      updatedAt: record.updatedAt,
      status: _statusFromName(record.status),
      retryCount: record.retryCount.clamp(0, maxPendingTaskRetries),
      lastError: record.lastError,
    );
  }

  PendingTaskStatus _statusFromName(String name) {
    return PendingTaskStatus.values.firstWhere(
      (status) => status.name == name,
      orElse: () => PendingTaskStatus.pending,
    );
  }
}
