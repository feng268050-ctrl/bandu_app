import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';

const bankQuestionTypeDisplayOrder = <BankQuestionType>[
  BankQuestionType.singleChoice,
  BankQuestionType.multipleChoice,
  BankQuestionType.fillBlank,
  BankQuestionType.subjective,
  BankQuestionType.unknown,
];

String bankQuestionTypeLabel(BankQuestionType type) => switch (type) {
      BankQuestionType.singleChoice => '单选题',
      BankQuestionType.multipleChoice => '多选题',
      BankQuestionType.fillBlank => '填空题',
      BankQuestionType.subjective => '大题',
      BankQuestionType.unknown => '其他题',
    };

String bankQuestionTypeSummary(
  Map<BankQuestionType, int> counts, {
  bool includeTotal = true,
}) {
  final parts = <String>[
    for (final type in bankQuestionTypeDisplayOrder)
      if ((counts[type] ?? 0) > 0)
        '${bankQuestionTypeLabel(type)} ${counts[type]} 道',
  ];
  if (includeTotal) {
    final total = counts.values.fold<int>(0, (sum, count) => sum + count);
    parts.add('共 $total 题');
  }
  return parts.join(' · ');
}
