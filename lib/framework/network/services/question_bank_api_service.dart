import 'package:bandu_wrong_notebook/conversion/api/question_bank/pdf_question_bank_dto_mapper.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/framework/network/client/api_client.dart';
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
  }) async {
    final formData = FormData.fromMap({
      'pdf': await MultipartFile.fromFile(
        localPdfPath,
        filename: p.basename(localPdfPath),
        contentType: DioMediaType.parse('application/pdf'),
      ),
      if (modelId != null) 'modelId': modelId,
    });
    final payload = await _apiClient.post<Object?>(
      '/question-banks/import',
      data: formData,
      receiveTimeout: const Duration(minutes: 5),
    );
    return PdfImportPreviewDto.fromJson(
      requireJsonObject(payload, context: 'PDF question bank response'),
    );
  }
}
