import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';

abstract interface class ErrorItemRepository {
  Future<List<ErrorItemSummary>> fetchErrorItems();

  Future<ErrorItemDetail> fetchErrorItem(String id);

  Future<ErrorItemDetail> updateErrorItem(String id, ErrorItemUpdate update);

  Future<void> deleteErrorItem(String id);
}
