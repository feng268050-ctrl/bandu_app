import 'dart:convert';
import 'dart:io';

import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/question_bank_cache_mapper.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class LocalQuestionBankStore {
  const LocalQuestionBankStore({
    this.mapper = const QuestionBankCacheMapper(),
  });

  final QuestionBankCacheMapper mapper;

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
}
