import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item_repository.dart';

class FetchErrorItemsUseCase {
  const FetchErrorItemsUseCase(this._repository);

  final ErrorItemRepository _repository;

  Future<List<ErrorItemSummary>> call() {
    return _repository.fetchErrorItems();
  }
}

class FetchErrorItemDetailUseCase {
  const FetchErrorItemDetailUseCase(this._repository);

  final ErrorItemRepository _repository;

  Future<ErrorItemDetail> call(String id) {
    return _repository.fetchErrorItem(id);
  }
}

class UpdateErrorItemUseCase {
  const UpdateErrorItemUseCase(this._repository);

  final ErrorItemRepository _repository;

  Future<ErrorItemDetail> call(String id, ErrorItemUpdate update) {
    return _repository.updateErrorItem(id, update);
  }
}

class DeleteErrorItemUseCase {
  const DeleteErrorItemUseCase(this._repository);

  final ErrorItemRepository _repository;

  Future<void> call(String id) {
    return _repository.deleteErrorItem(id);
  }
}
