import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';
import 'package:bandu_wrong_notebook/conversion/persistence/question_bank_cache_mapper.dart';

class ExamAttemptRecord {
  const ExamAttemptRecord({
    required this.id,
    required this.question,
    required this.orderIndex,
    required this.answerRevealed,
    required this.gradingResult,
    required this.gradingSource,
    this.userAnswer,
    this.gradingFeedback,
    this.submittedAt,
  });

  factory ExamAttemptRecord.fromJson(JsonObject json) {
    return ExamAttemptRecord(
      id: json['id']?.toString() ?? '',
      question: BankQuestionRecord.fromJson(
        requireJsonObject(json['question'], context: 'exam question'),
      ),
      orderIndex: intValue(json['orderIndex']),
      userAnswer: json['userAnswer']?.toString(),
      answerRevealed: boolValue(json['answerRevealed']),
      gradingResult: json['gradingResult']?.toString() ?? 'notGraded',
      gradingSource: json['gradingSource']?.toString() ?? 'none',
      gradingFeedback: json['gradingFeedback']?.toString(),
      submittedAt: nullableDateTimeValue(json['submittedAt']),
    );
  }

  final String id;
  final BankQuestionRecord question;
  final int orderIndex;
  final String? userAnswer;
  final bool answerRevealed;
  final String gradingResult;
  final String gradingSource;
  final String? gradingFeedback;
  final DateTime? submittedAt;

  JsonObject toJson() => {
        'id': id,
        'question': question.toJson(),
        'orderIndex': orderIndex,
        'userAnswer': userAnswer,
        'answerRevealed': answerRevealed,
        'gradingResult': gradingResult,
        'gradingSource': gradingSource,
        'gradingFeedback': gradingFeedback,
        'submittedAt': submittedAt?.toIso8601String(),
      };
}

class ExamSessionRecord {
  const ExamSessionRecord({
    required this.id,
    required this.questionSetId,
    required this.title,
    required this.seed,
    required this.status,
    required this.attempts,
    required this.createdAt,
    this.completedAt,
  });

  factory ExamSessionRecord.fromJson(JsonObject json) {
    final rawAttempts = json['attempts'];
    return ExamSessionRecord(
      id: json['id']?.toString() ?? '',
      questionSetId: json['questionSetId']?.toString() ?? '',
      title: json['title']?.toString() ?? '随机考卷',
      seed: intValue(json['seed']),
      status: json['status']?.toString() ?? 'inProgress',
      attempts: rawAttempts is List
          ? rawAttempts
              .whereType<JsonObject>()
              .map(ExamAttemptRecord.fromJson)
              .toList()
          : const [],
      createdAt: dateTimeValue(json['createdAt']),
      completedAt: nullableDateTimeValue(json['completedAt']),
    );
  }

  final String id;
  final String questionSetId;
  final String title;
  final int seed;
  final String status;
  final List<ExamAttemptRecord> attempts;
  final DateTime createdAt;
  final DateTime? completedAt;

  JsonObject toJson() => {
        'id': id,
        'questionSetId': questionSetId,
        'title': title,
        'seed': seed,
        'status': status,
        'attempts': attempts.map((attempt) => attempt.toJson()).toList(),
        'createdAt': createdAt.toIso8601String(),
        'completedAt': completedAt?.toIso8601String(),
      };
}

class ExamSessionCacheMapper {
  const ExamSessionCacheMapper({
    this.questionMapper = const QuestionBankCacheMapper(),
  });

  final QuestionBankCacheMapper questionMapper;

  ExamSession fromRecord(ExamSessionRecord record) {
    return ExamSession(
      id: record.id,
      questionSetId: record.questionSetId,
      title: record.title,
      seed: record.seed,
      status: ExamSessionStatus.values.firstWhere(
        (value) => value.name == record.status,
        orElse: () => ExamSessionStatus.inProgress,
      ),
      attempts: record.attempts.map(_attemptFromRecord).toList(),
      createdAt: record.createdAt,
      completedAt: record.completedAt,
    );
  }

  ExamSessionRecord toRecord(ExamSession session) {
    return ExamSessionRecord(
      id: session.id,
      questionSetId: session.questionSetId,
      title: session.title,
      seed: session.seed,
      status: session.status.name,
      attempts: session.attempts.map(_attemptToRecord).toList(),
      createdAt: session.createdAt,
      completedAt: session.completedAt,
    );
  }

  ExamAttempt _attemptFromRecord(ExamAttemptRecord record) {
    return ExamAttempt(
      id: record.id,
      question: questionMapper.questionFromRecord(record.question),
      orderIndex: record.orderIndex,
      userAnswer: record.userAnswer,
      answerRevealed: record.answerRevealed,
      gradingResult: ExamGradingResult.values.firstWhere(
        (value) => value.name == record.gradingResult,
        orElse: () => ExamGradingResult.notGraded,
      ),
      gradingSource: ExamGradingSource.values.firstWhere(
        (value) => value.name == record.gradingSource,
        orElse: () => ExamGradingSource.none,
      ),
      gradingFeedback: record.gradingFeedback,
      submittedAt: record.submittedAt,
    );
  }

  ExamAttemptRecord _attemptToRecord(ExamAttempt attempt) {
    return ExamAttemptRecord(
      id: attempt.id,
      question: questionMapper.questionToRecord(attempt.question),
      orderIndex: attempt.orderIndex,
      userAnswer: attempt.userAnswer,
      answerRevealed: attempt.answerRevealed,
      gradingResult: attempt.gradingResult.name,
      gradingSource: attempt.gradingSource.name,
      gradingFeedback: attempt.gradingFeedback,
      submittedAt: attempt.submittedAt,
    );
  }
}
