import 'package:bandu_wrong_notebook/application/features/practice/domain/practice_models.dart';
import 'package:bandu_wrong_notebook/conversion/common/json_value.dart';
import 'package:bandu_wrong_notebook/conversion/common/value_converter.dart';

class GeneratePracticeRequestDto {
  const GeneratePracticeRequestDto({
    required this.errorItemId,
    required this.difficulty,
    this.language = 'zh',
  });

  final String errorItemId;
  final String difficulty;
  final String language;

  JsonObject toJson() => {
        'errorItemId': errorItemId,
        'difficulty': difficulty,
        'language': language,
      };
}

class RecordPracticeRequestDto {
  const RecordPracticeRequestDto({
    required this.subject,
    required this.difficulty,
    required this.isCorrect,
  });

  final String subject;
  final String difficulty;
  final bool isCorrect;

  JsonObject toJson() => {
        'subject': subject,
        'difficulty': difficulty,
        'isCorrect': isCorrect,
      };
}

class PracticeQuestionDto {
  const PracticeQuestionDto({
    required this.title,
    required this.questionText,
    required this.answer,
    required this.analysis,
    required this.subjectName,
    this.tags = const [],
  });

  factory PracticeQuestionDto.fromJson(JsonObject json) {
    return PracticeQuestionDto(
      title: json['title']?.toString() ?? '练习题',
      questionText: json['questionText']?.toString() ?? '',
      answer: json['answer']?.toString() ?? '',
      analysis: json['analysis']?.toString() ?? '',
      subjectName: json['subjectName']?.toString() ?? '其他',
      tags: stringListValue(json['tags']),
    );
  }

  final String title;
  final String questionText;
  final String answer;
  final String analysis;
  final String subjectName;
  final List<String> tags;
}

class PracticeRecordDto {
  const PracticeRecordDto({
    required this.id,
    required this.subject,
    required this.difficulty,
    required this.isCorrect,
    required this.createdAt,
  });

  factory PracticeRecordDto.fromJson(JsonObject json) {
    return PracticeRecordDto(
      id: json['id']?.toString() ?? '',
      subject: json['subject']?.toString() ?? '未分类',
      difficulty: json['difficulty']?.toString() ?? 'medium',
      isCorrect: json['isCorrect'] == true,
      createdAt: dateTimeValue(json['createdAt']),
    );
  }

  final String id;
  final String subject;
  final String difficulty;
  final bool isCorrect;
  final DateTime createdAt;
}

class PracticeDtoMapper {
  const PracticeDtoMapper();

  GeneratePracticeRequestDto generateRequest({
    required String errorItemId,
    required String difficulty,
  }) {
    return GeneratePracticeRequestDto(
      errorItemId: errorItemId,
      difficulty: difficulty,
    );
  }

  RecordPracticeRequestDto recordRequest({
    required String subject,
    required String difficulty,
    required bool isCorrect,
  }) {
    return RecordPracticeRequestDto(
      subject: subject,
      difficulty: difficulty,
      isCorrect: isCorrect,
    );
  }

  PracticeQuestion questionFromDto(PracticeQuestionDto dto) {
    return PracticeQuestion(
      title: dto.title,
      questionText: dto.questionText,
      answer: dto.answer,
      analysis: dto.analysis,
      subjectName: dto.subjectName,
      tags: dto.tags,
    );
  }

  List<PracticeRecord> historyFromDtos(List<PracticeRecordDto> dtos) {
    return dtos.map((dto) {
      return PracticeRecord(
        id: dto.id,
        subject: dto.subject,
        difficulty: dto.difficulty,
        isCorrect: dto.isCorrect,
        createdAt: dto.createdAt,
      );
    }).toList();
  }
}
