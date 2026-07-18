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

final renamePdfQuestionSetUseCaseProvider =
    Provider<RenamePdfQuestionSetUseCase>(
  (ref) =>
      RenamePdfQuestionSetUseCase(ref.watch(questionBankRepositoryProvider)),
);

final deletePdfQuestionSetUseCaseProvider =
    Provider<DeletePdfQuestionSetUseCase>(
  (ref) =>
      DeletePdfQuestionSetUseCase(ref.watch(questionBankRepositoryProvider)),
);

final createRandomExamUseCaseProvider = Provider<CreateRandomExamUseCase>(
  (ref) => CreateRandomExamUseCase(ref.watch(questionBankRepositoryProvider)),
);

final loadExamSessionUseCaseProvider = Provider<LoadExamSessionUseCase>(
  (ref) => LoadExamSessionUseCase(ref.watch(questionBankRepositoryProvider)),
);

final loadExamSessionsUseCaseProvider = Provider<LoadExamSessionsUseCase>(
  (ref) => LoadExamSessionsUseCase(ref.watch(questionBankRepositoryProvider)),
);

final renameExamSessionUseCaseProvider = Provider<RenameExamSessionUseCase>(
  (ref) => RenameExamSessionUseCase(ref.watch(questionBankRepositoryProvider)),
);

final deleteExamSessionUseCaseProvider = Provider<DeleteExamSessionUseCase>(
  (ref) => DeleteExamSessionUseCase(ref.watch(questionBankRepositoryProvider)),
);

final submitExamAnswerUseCaseProvider = Provider<SubmitExamAnswerUseCase>(
  (ref) => SubmitExamAnswerUseCase(ref.watch(questionBankRepositoryProvider)),
);
