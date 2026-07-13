import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_models.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_repositories.dart';
import 'package:bandu_wrong_notebook/conversion/api/tutor/tutor_dto_mapper.dart';
import 'package:bandu_wrong_notebook/framework/network/services/tutor_api_service.dart';
import 'package:bandu_wrong_notebook/framework/persistence/tutor/local_tutor_session_store.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

final localTutorSessionStoreProvider = Provider<LocalTutorSessionStore>((ref) {
  return const LocalTutorSessionStore();
});

final frameworkTutorRepositoryProvider = Provider<TutorRepository>((ref) {
  return FrameworkTutorRepository(
    apiService: ref.watch(tutorApiServiceProvider),
    store: ref.watch(localTutorSessionStoreProvider),
    imagePicker: ImagePicker(),
    mapper: const TutorDtoMapper(),
  );
});

class FrameworkTutorRepository implements TutorRepository {
  const FrameworkTutorRepository({
    required this.apiService,
    required this.store,
    required this.imagePicker,
    required this.mapper,
  });

  final TutorApiService apiService;
  final LocalTutorSessionStore store;
  final ImagePicker imagePicker;
  final TutorDtoMapper mapper;

  @override
  Future<List<TutorModel>> fetchModels() async {
    final dtos = await apiService.fetchModels();
    return dtos.map(mapper.modelFromDto).toList();
  }

  @override
  Future<List<TutorSession>> loadSessions() => store.load();

  @override
  Future<void> saveSession(TutorSession session) => store.save(session);

  @override
  Future<void> deleteSession(String sessionId) => store.delete(sessionId);

  @override
  Future<String?> pickImage() async {
    final image = await imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 90,
      maxWidth: 2000,
    );
    if (image == null) return null;
    return store.persistAttachment(image.path);
  }

  @override
  Future<TutorReply> sendMessage({
    required String message,
    required List<TutorMessage> history,
    String? modelId,
    TutorQuestionContext? questionContext,
    String? imagePath,
  }) async {
    final dto = await apiService.sendMessage(
      message: message,
      history: history.map(mapper.historyToDto).toList(),
      modelId: modelId,
      questionContext: questionContext?.content,
      imagePath: imagePath,
    );
    return mapper.replyFromDto(dto);
  }
}
