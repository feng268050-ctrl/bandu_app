import 'dart:convert';

import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/conversion/api/tutor/tutor_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

final tutorApiServiceProvider = Provider<TutorApiService>((ref) {
  return TutorApiService(ref.watch(apiClientProvider));
});

class TutorApiService {
  const TutorApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<List<TutorModelDto>> fetchModels() async {
    final payload = await _apiClient.get<Object?>('tutor/models');
    return requireJsonArray(payload, context: 'tutor models response')
        .map((item) => requireJsonObject(item, context: 'tutor model'))
        .map(TutorModelDto.fromJson)
        .toList();
  }

  Future<TutorReplyDto> sendMessage({
    required String message,
    required List<TutorHistoryMessageDto> history,
    String? modelId,
    AiPurposePreference? preference,
    String? questionContext,
    String? imagePath,
  }) async {
    final selection = preference ??
        (modelId == null
            ? const AiPurposePreference(purpose: AiPurpose.tutor)
            : AiPurposePreference(
                purpose: AiPurpose.tutor,
                mode: AiSelectionMode.manual,
                selectedModelId: modelId,
              ));
    final formData = FormData.fromMap({
      'message': message,
      'history': jsonEncode(history.map((item) => item.toJson()).toList()),
      'purpose': 'TUTOR',
      'modelSelection': jsonEncode(selection.toRequestSelection()),
      if (questionContext != null) 'questionContext': questionContext,
      if (imagePath != null)
        'image': await MultipartFile.fromFile(
          imagePath,
          filename: p.basename(imagePath),
        ),
    });
    final payload = await _apiClient.post<Object?>(
      'tutor/messages',
      data: formData,
      timeouts: aiRequestTimeouts,
    );
    return TutorReplyDto.fromJson(
      requireJsonObject(payload, context: 'tutor message response'),
    );
  }
}
