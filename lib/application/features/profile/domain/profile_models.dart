import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';

enum ProfileSection {
  student,
  ai,
  device,
  settings,
  pendingTasks,
  network,
  data,
  about,
}

const visibleProfileSections = [
  ProfileSection.student,
  ProfileSection.ai,
  ProfileSection.device,
  ProfileSection.settings,
  ProfileSection.data,
  ProfileSection.about,
];

class StudentProfileSummary {
  const StudentProfileSummary({
    required this.nickname,
    required this.educationStageLabel,
    required this.gradeLabel,
  });

  factory StudentProfileSummary.fromUser(UserProfile? user) {
    final nickname = _displayName(user);
    final stageLabel = _educationStageLabel(user?.educationStage);
    final gradeLabel = _gradeLabel(user?.educationStage, user?.enrollmentYear);

    return StudentProfileSummary(
      nickname: nickname,
      educationStageLabel: stageLabel,
      gradeLabel: gradeLabel,
    );
  }

  final String nickname;
  final String? educationStageLabel;
  final String? gradeLabel;

  String get headline {
    final parts = [
      educationStageLabel,
      gradeLabel,
    ].whereType<String>().where((part) => part.isNotEmpty).toList();

    if (parts.isEmpty) {
      return '未设置教育阶段和年级';
    }

    return parts.join(' · ');
  }

  String get initial {
    final trimmed = nickname.trim();
    if (trimmed.isEmpty) {
      return '伴';
    }
    return trimmed.substring(0, 1);
  }
}

class ProfileOverviewState {
  const ProfileOverviewState({
    required this.summary,
    required this.modelConfigLabel,
    required this.deviceNameLabel,
    required this.sections,
  });

  factory ProfileOverviewState.fromUser(
    UserProfile? user, {
    String deviceNameLabel = '读取中',
    String modelConfigLabel = '读取中',
  }) {
    return ProfileOverviewState(
      summary: StudentProfileSummary.fromUser(user),
      modelConfigLabel: modelConfigLabel,
      deviceNameLabel: deviceNameLabel,
      sections: visibleProfileSections,
    );
  }

  final StudentProfileSummary summary;
  final String modelConfigLabel;
  final String deviceNameLabel;
  final List<ProfileSection> sections;
}

String _displayName(UserProfile? user) {
  final name = user?.name?.trim();
  if (name != null && name.isNotEmpty) {
    return name;
  }

  final email = user?.email.trim();
  if (email != null && email.isNotEmpty) {
    return email.split('@').first;
  }

  return '未设置昵称';
}

String? _educationStageLabel(String? value) {
  return switch (value) {
    'primary' || '小学' => '小学',
    'junior_high' || '初中' => '初中',
    'senior_high' || '高中' => '高中',
    'university' || '大学' => '大学',
    final String text when text.trim().isNotEmpty => text,
    _ => null,
  };
}

String? _gradeLabel(String? educationStage, int? enrollmentYear) {
  if (enrollmentYear == null) {
    return null;
  }

  final now = DateTime.now();
  final yearIndex = now.year - enrollmentYear;

  if (yearIndex < 1) {
    return null;
  }

  final maxYear = switch (educationStage) {
    'primary' || '小学' => 6,
    'junior_high' || '初中' => 3,
    'senior_high' || '高中' => 3,
    'university' || '大学' => 4,
    _ => null,
  };

  if (maxYear != null && yearIndex > maxYear) {
    return null;
  }

  return '$yearIndex年级';
}
