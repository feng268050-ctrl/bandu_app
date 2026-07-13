import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/cache_records.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/error_item_cache_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('error item detail survives domain-cache-json round trip', () {
    const mapper = ErrorItemCacheMapper();
    final detail = ErrorItemDetail(
      id: 'e1',
      title: '分数题',
      subjectName: '数学',
      questionText: '1/2 + 1/2',
      answer: '1',
      analysis: '同分母相加',
      masteryLevel: 2,
      updatedAt: DateTime.utc(2026, 7, 13),
    );

    final record = mapper.detailToCache(detail);
    final decoded = CachedErrorItemDetail.fromJson(record.toJson());
    final restored = mapper.detailFromCache(decoded);

    expect(restored.id, detail.id);
    expect(restored.answer, detail.answer);
    expect(restored.masteryLevel, 2);
    expect(restored.updatedAt, DateTime.utc(2026, 7, 13));
  });
}
