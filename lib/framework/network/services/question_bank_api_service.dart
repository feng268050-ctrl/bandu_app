import 'package:bandu_wrong_notebook/conversion/api/question_bank/pdf_question_bank_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;

final questionBankApiServiceProvider = Provider<QuestionBankApiService>((ref) {
  return QuestionBankApiService(ref.watch(apiClientProvider));
});

class QuestionBankApiService {
  const QuestionBankApiService(this._apiClient);

  final ApiClient _apiClient;

  Future<PdfImportPreviewDto> analyzePdf(
    String localPdfPath, {
    String? modelId,
    AiPurposePreference? preference,
  }) async {
    final selection = preference ??
        (modelId == null
            ? const AiPurposePreference(purpose: AiPurpose.pdfImport)
            : AiPurposePreference(
                purpose: AiPurpose.pdfImport,
                mode: AiSelectionMode.manual,
                selectedModelId: modelId,
              ));
    final formData = FormData.fromMap({
      'pdf': await MultipartFile.fromFile(
        localPdfPath,
        filename: p.basename(localPdfPath),
        contentType: DioMediaType.parse('application/pdf'),
      ),
      'purpose': 'PDF_IMPORT',
      'modelSelection': jsonEncode(selection.toRequestSelection()),
    });
    final payload = await _apiClient.post<Object?>(
      'question-banks/import',
      data: formData,
      timeouts: const ApiRequestTimeouts(
        connect: Duration(seconds: 15),
        send: Duration(minutes: 2),
        receive: Duration(minutes: 5),
      ),
    );
    return PdfImportPreviewDto.fromJson(
      requireJsonObject(payload, context: 'PDF question bank response'),
    );
  }
}
