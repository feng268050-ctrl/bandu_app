import 'package:bandu_wrong_notebook/features/library/application/error_item_use_cases.dart';
import 'package:bandu_wrong_notebook/features/library/data/error_item_repository_impl.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final fetchErrorItemsUseCaseProvider = Provider<FetchErrorItemsUseCase>((ref) {
  return FetchErrorItemsUseCase(ref.watch(errorItemRepositoryProvider));
});

final fetchErrorItemDetailUseCaseProvider =
    Provider<FetchErrorItemDetailUseCase>((ref) {
  return FetchErrorItemDetailUseCase(ref.watch(errorItemRepositoryProvider));
});
