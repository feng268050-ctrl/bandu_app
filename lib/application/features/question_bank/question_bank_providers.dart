import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/application/question_bank_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final questionBankRepositoryProvider = Provider<QuestionBankRepository>(
  (ref) => missingDependency('QuestionBankRepository'),
);

final pickPdfQuestionSetUseCaseProvider = Provider<PickPdfQuestionSetUseCase>(
  (ref) => PickPdfQuestionSetUseCase(ref.watch(questionBankRepositoryProvider)),
);

final analyzePdfQuestionSetUseCaseProvider =
    Provider<AnalyzePdfQuestionSetUseCase>(
  (ref) => AnalyzePdfQuestionSetUseCase(
    ref.watch(questionBankRepositoryProvider),
  ),
);

final loadPdfQuestionSetsUseCaseProvider = Provider<LoadPdfQuestionSetsUseCase>(
  (ref) =>
      LoadPdfQuestionSetsUseCase(ref.watch(questionBankRepositoryProvider)),
);

final savePdfQuestionSetUseCaseProvider = Provider<SavePdfQuestionSetUseCase>(
  (ref) => SavePdfQuestionSetUseCase(ref.watch(questionBankRepositoryProvider)),
);
