import 'package:bandu_wrong_notebook/core/database/app_database.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';

class ErrorItemDtoMapper {
  const ErrorItemDtoMapper();

  List<ErrorItemSummary> summaryListFromJson(List<Object?> data) {
    return data
        .whereType<Map<String, Object?>>()
        .map(ErrorItemSummary.fromJson)
        .toList();
  }

  ErrorItemDetail detailFromJson(Map<String, Object?> data) {
    return ErrorItemDetail.fromJson(data);
  }

  CachedErrorItem summaryToCache(ErrorItemSummary item) {
    return CachedErrorItem(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      updatedAt: item.updatedAt,
      mastered: item.mastered,
    );
  }

  ErrorItemSummary summaryFromCache(CachedErrorItem item) {
    return ErrorItemSummary(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      updatedAt: item.updatedAt,
      mastered: item.mastered,
    );
  }
}
