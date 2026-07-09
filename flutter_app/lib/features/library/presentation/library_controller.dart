import 'package:bandu_wrong_notebook/features/library/data/error_item_repository.dart';
import 'package:bandu_wrong_notebook/features/library/domain/error_item.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final libraryControllerProvider =
    AsyncNotifierProvider<LibraryController, List<ErrorItemSummary>>(
  LibraryController.new,
);

class LibraryController extends AsyncNotifier<List<ErrorItemSummary>> {
  ErrorItemRepository get _repository => ref.read(errorItemRepositoryProvider);

  @override
  Future<List<ErrorItemSummary>> build() {
    return _repository.fetchErrorItems();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(_repository.fetchErrorItems);
  }
}
