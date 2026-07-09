import 'package:bandu_wrong_notebook/features/auth/domain/auth_models.dart';
import 'package:flutter/material.dart';

enum ProfileSection {
  student(
    title: '学生资料',
    description: '昵称、教育阶段和入学年份',
    icon: Icons.school_outlined,
  ),
  ai(
    title: 'AI 配置',
    description: '服务、模型、密钥和提示词',
    icon: Icons.auto_awesome_outlined,
  ),
  device(
    title: '设备名称',
    description: '附近设备和配对时显示的名称',
    icon: Icons.devices_outlined,
  ),
  data(
    title: '数据管理',
    description: '清除学习数据或恢复出厂设置',
    icon: Icons.storage_outlined,
  ),
  about(
    title: '关于',
    description: '版本、隐私、许可和图标来源',
    icon: Icons.info_outline,
  );

  const ProfileSection({
    required this.title,
    required this.description,
    required this.icon,
  });

  final String title;
  final String description;
  final IconData icon;
}

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

  factory ProfileOverviewState.fromUser(UserProfile? user) {
    return ProfileOverviewState(
      summary: StudentProfileSummary.fromUser(user),
      modelConfigLabel: '后端托管',
      deviceNameLabel: '未设置',
      sections: ProfileSection.values,
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
  var yearIndex = now.year - enrollmentYear + 1;
  if (now.month < 9) {
    yearIndex -= 1;
  }

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
