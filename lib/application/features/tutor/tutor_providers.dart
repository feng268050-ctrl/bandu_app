import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/application/tutor_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_repositories.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final tutorRepositoryProvider = Provider<TutorRepository>(
  (ref) => missingDependency('TutorRepository'),
);

final speechInputRepositoryProvider = Provider<SpeechInputRepository>(
  (ref) => missingDependency('SpeechInputRepository'),
);

final loadTutorModelsUseCaseProvider = Provider<LoadTutorModelsUseCase>(
  (ref) => LoadTutorModelsUseCase(ref.watch(tutorRepositoryProvider)),
);
final loadTutorSessionsUseCaseProvider = Provider<LoadTutorSessionsUseCase>(
  (ref) => LoadTutorSessionsUseCase(ref.watch(tutorRepositoryProvider)),
);
final saveTutorSessionUseCaseProvider = Provider<SaveTutorSessionUseCase>(
  (ref) => SaveTutorSessionUseCase(ref.watch(tutorRepositoryProvider)),
);
final deleteTutorSessionUseCaseProvider = Provider<DeleteTutorSessionUseCase>(
  (ref) => DeleteTutorSessionUseCase(ref.watch(tutorRepositoryProvider)),
);
final pickTutorImageUseCaseProvider = Provider<PickTutorImageUseCase>(
  (ref) => PickTutorImageUseCase(ref.watch(tutorRepositoryProvider)),
);
final sendTutorMessageUseCaseProvider = Provider<SendTutorMessageUseCase>(
  (ref) => SendTutorMessageUseCase(ref.watch(tutorRepositoryProvider)),
);
final startSpeechInputUseCaseProvider = Provider<StartSpeechInputUseCase>(
  (ref) => StartSpeechInputUseCase(ref.watch(speechInputRepositoryProvider)),
);
final stopSpeechInputUseCaseProvider = Provider<StopSpeechInputUseCase>(
  (ref) => StopSpeechInputUseCase(ref.watch(speechInputRepositoryProvider)),
);
