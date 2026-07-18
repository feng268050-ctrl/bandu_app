import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/exam_session_cache_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/question_bank_cache_mapper.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class LocalQuestionBankStore {
  const LocalQuestionBankStore({
    this.mapper = const QuestionBankCacheMapper(),
    this.examMapper = const ExamSessionCacheMapper(),
  });

  final QuestionBankCacheMapper mapper;
  final ExamSessionCacheMapper examMapper;

  Future<List<PdfQuestionSet>> load() async {
    final file = await _indexFile();
    if (!await file.exists()) return const [];
    final decoded = jsonDecode(await file.readAsString());
    if (decoded is! List) return const [];
    return decoded
        .whereType<JsonObject>()
        .map(PdfQuestionSetRecord.fromJson)
        .map(mapper.fromRecord)
        .toList()
      ..sort((left, right) => right.importedAt.compareTo(left.importedAt));
  }

  Future<PdfQuestionSet> save(PdfImportPreview preview) async {
    final importedAt = DateTime.now();
    final id = importedAt.microsecondsSinceEpoch.toString();
    final directory = await _directory();
    final targetPdf = File(p.join(directory.path, '$id.pdf'));
    final sourcePdf = File(preview.localPdfPath);
    if (sourcePdf.absolute.path != targetPdf.absolute.path) {
      await sourcePdf.copy(targetPdf.path);
    }
    final set = PdfQuestionSet(
      id: id,
      name: p.basenameWithoutExtension(preview.fileName),
      sourceFileName: preview.fileName,
      localPdfPath: targetPdf.path,
      importedAt: importedAt,
      questions: [
        for (var index = 0; index < preview.questions.length; index++)
          _withPersistentId(preview.questions[index], '$id-$index'),
      ],
    );
    final sets = await load();
    await _write([set, ...sets.where((item) => item.id != id)]);
    return set;
  }

  Future<PdfQuestionSet> rename(String questionSetId, String name) async {
    final normalizedName = name.trim();
    if (normalizedName.isEmpty) {
      throw ArgumentError.value(name, 'name');
    }
    final sets = await load();
    final index = sets.indexWhere((item) => item.id == questionSetId);
    if (index < 0) {
      throw StateError('question_bank_not_found');
    }
    final updated = sets[index].copyWith(name: normalizedName);
    sets[index] = updated;
    await _write(sets);
    return updated;
  }

  Future<void> delete(String questionSetId) async {
    final sets = await load();
    final target = sets.where((item) => item.id == questionSetId).firstOrNull;
    if (target == null) {
      throw StateError('question_bank_not_found');
    }
    final remaining =
        sets.where((item) => item.id != questionSetId).toList(growable: false);
    await _write(remaining);

    final pdf = File(target.localPdfPath);
    if (await pdf.exists()) {
      await pdf.delete();
    }

    final sessions = await loadExamSessions();
    final keptSessions = sessions
        .where((session) => session.questionSetId != questionSetId)
        .toList(growable: false);
    if (keptSessions.length != sessions.length) {
      await _writeExamSessions(keptSessions);
    }
  }

  Future<ExamSession> createExam(
    String questionSetId,
    Map<BankQuestionType, int> questionCounts,
  ) async {
    final questionSets = await load();
    final questionSet =
        questionSets.where((item) => item.id == questionSetId).firstOrNull;
    if (questionSet == null) {
      throw StateError('question_bank_not_found');
    }

    final createdAt = DateTime.now();
    final sessionId = 'exam-${createdAt.microsecondsSinceEpoch}';
    final session = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: questionCounts,
      sessionId: sessionId,
      seed: createdAt.microsecondsSinceEpoch,
      createdAt: createdAt,
    );
    final sessions = await loadExamSessions();
    await _writeExamSessions([
      session,
      ...sessions.where((item) => item.id != session.id),
    ]);
    return session;
  }

  Future<ExamSession?> loadExamSession(String sessionId) async {
    final sessions = await loadExamSessions();
    return sessions.where((item) => item.id == sessionId).firstOrNull;
  }

  Future<ExamSession> renameExamSession(
    String sessionId,
    String title,
  ) async {
    final normalizedTitle = title.trim();
    if (normalizedTitle.isEmpty) {
      throw ArgumentError.value(title, 'title');
    }
    final sessions = await loadExamSessions();
    final sessionIndex = sessions.indexWhere((item) => item.id == sessionId);
    if (sessionIndex < 0) {
      throw StateError('exam_session_not_found');
    }
    final updated = sessions[sessionIndex].copyWith(title: normalizedTitle);
    sessions[sessionIndex] = updated;
    await _writeExamSessions(sessions);
    return updated;
  }

  Future<void> deleteExamSession(String sessionId) async {
    final sessions = await loadExamSessions();
    final remaining = sessions.where((item) => item.id != sessionId).toList();
    if (remaining.length == sessions.length) {
      throw StateError('exam_session_not_found');
    }
    await _writeExamSessions(remaining);
  }

  Future<ExamSession> submitExamAnswer(
    String sessionId,
    String attemptId,
    String userAnswer,
  ) async {
    final sessions = await loadExamSessions();
    final sessionIndex = sessions.indexWhere((item) => item.id == sessionId);
    if (sessionIndex < 0) {
      throw StateError('exam_session_not_found');
    }
    final session = sessions[sessionIndex];
    final attemptIndex =
        session.attempts.indexWhere((item) => item.id == attemptId);
    if (attemptIndex < 0) {
      throw StateError('exam_attempt_not_found');
    }

    final answer = userAnswer.trim();
    if (answer.isEmpty) {
      throw ArgumentError.value(userAnswer, 'userAnswer');
    }
    final now = DateTime.now();
    final attempts = [...session.attempts];
    final attempt = attempts[attemptIndex];
    final grade = gradeBankQuestion(attempt.question, answer);
    attempts[attemptIndex] = attempt.copyWith(
      userAnswer: answer,
      answerRevealed: true,
      gradingResult: grade.result,
      gradingSource: grade.source,
      gradingFeedback: grade.feedback,
      submittedAt: now,
    );
    final completed = attempts.every((item) => item.answerRevealed);
    final updated = session.copyWith(
      attempts: attempts,
      status: completed
          ? ExamSessionStatus.completed
          : ExamSessionStatus.inProgress,
      completedAt: completed ? now : null,
    );
    sessions[sessionIndex] = updated;
    await _writeExamSessions(sessions);
    return updated;
  }

  Future<void> clear() async {
    final directory = await _directory();
    if (await directory.exists()) {
      await directory.delete(recursive: true);
    }
  }

  BankQuestion _withPersistentId(BankQuestion source, String id) {
    return BankQuestion(
      id: id,
      stem: source.stem,
      options: source.options,
      answer: source.answer,
      analysis: source.analysis,
      questionType: source.questionType,
      difficulty: source.difficulty,
      tags: source.tags,
      sourcePage: source.sourcePage,
      needsReview: source.needsReview,
    );
  }

  Future<void> _write(List<PdfQuestionSet> sets) async {
    final file = await _indexFile();
    await file.writeAsString(
      jsonEncode(
          sets.map(mapper.toRecord).map((item) => item.toJson()).toList()),
      flush: true,
    );
  }

  Future<List<ExamSession>> loadExamSessions() async {
    final file = await _examIndexFile();
    if (!await file.exists()) return [];
    final decoded = jsonDecode(await file.readAsString());
    if (decoded is! List) return [];
    return decoded
        .whereType<JsonObject>()
        .map(ExamSessionRecord.fromJson)
        .map(examMapper.fromRecord)
        .toList()
      ..sort((left, right) => right.createdAt.compareTo(left.createdAt));
  }

  Future<void> _writeExamSessions(List<ExamSession> sessions) async {
    final file = await _examIndexFile();
    await file.writeAsString(
      jsonEncode(
        sessions.map(examMapper.toRecord).map((item) => item.toJson()).toList(),
      ),
      flush: true,
    );
  }

  Future<Directory> _directory() async {
    final root = await getApplicationDocumentsDirectory();
    final directory = Directory(p.join(root.path, 'bandu', 'question_banks'));
    await directory.create(recursive: true);
    return directory;
  }

  Future<File> _indexFile() async {
    final directory = await _directory();
    return File(p.join(directory.path, 'question_sets_v1.json'));
  }

  Future<File> _examIndexFile() async {
    final directory = await _directory();
    return File(p.join(directory.path, 'exam_sessions_v1.json'));
  }
}
