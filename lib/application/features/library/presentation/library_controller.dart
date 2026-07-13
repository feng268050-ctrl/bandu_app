import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/application/features/library/library_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final libraryControllerProvider =
    AsyncNotifierProvider<LibraryController, List<ErrorItemSummary>>(
  LibraryController.new,
);

class LibraryController extends AsyncNotifier<List<ErrorItemSummary>> {
  @override
  Future<List<ErrorItemSummary>> build() {
    return ref.read(fetchErrorItemsUseCaseProvider).call();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      ref.read(fetchErrorItemsUseCaseProvider).call,
    );
  }
}
