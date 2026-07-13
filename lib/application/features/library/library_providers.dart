import 'package:bandu_wrong_notebook/application/app/missing_dependency.dart';
import 'package:bandu_wrong_notebook/application/features/library/application/error_item_use_cases.dart';
import 'package:bandu_wrong_notebook/application/features/library/domain/error_item_repository.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final errorItemRepositoryProvider = Provider<ErrorItemRepository>(
  (ref) => missingDependency('ErrorItemRepository'),
);

final fetchErrorItemsUseCaseProvider = Provider<FetchErrorItemsUseCase>((ref) {
  return FetchErrorItemsUseCase(ref.watch(errorItemRepositoryProvider));
});

final fetchErrorItemDetailUseCaseProvider =
    Provider<FetchErrorItemDetailUseCase>((ref) {
  return FetchErrorItemDetailUseCase(
    ref.watch(errorItemRepositoryProvider),
  );
});

final updateErrorItemUseCaseProvider = Provider<UpdateErrorItemUseCase>((ref) {
  return UpdateErrorItemUseCase(ref.watch(errorItemRepositoryProvider));
});

final deleteErrorItemUseCaseProvider = Provider<DeleteErrorItemUseCase>((ref) {
  return DeleteErrorItemUseCase(ref.watch(errorItemRepositoryProvider));
});
