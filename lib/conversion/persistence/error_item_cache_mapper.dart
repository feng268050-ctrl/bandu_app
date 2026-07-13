import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';

class ErrorItemCacheMapper {
  const ErrorItemCacheMapper();

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

  CachedErrorItemDetail detailToCache(ErrorItemDetail item) {
    return CachedErrorItemDetail(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      questionText: item.questionText,
      answer: item.answer,
      analysis: item.analysis,
      masteryLevel: item.masteryLevel,
      updatedAt: item.updatedAt,
    );
  }

  ErrorItemDetail detailFromCache(CachedErrorItemDetail item) {
    return ErrorItemDetail(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      questionText: item.questionText,
      answer: item.answer,
      analysis: item.analysis,
      masteryLevel: item.masteryLevel,
      updatedAt: item.updatedAt,
    );
  }

  CachedErrorItemDetail savedItemToCache(SavedErrorItem item) {
    return CachedErrorItemDetail(
      id: item.id,
      title: item.title,
      subjectName: item.subjectName,
      questionText: item.questionText,
      answer: item.answer,
      analysis: item.analysis,
      updatedAt: item.updatedAt ?? DateTime.now(),
    );
  }
}
