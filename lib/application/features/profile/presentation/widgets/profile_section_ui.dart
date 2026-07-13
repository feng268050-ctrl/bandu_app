import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:flutter/material.dart';

extension ProfileSectionUi on ProfileSection {
  String get title => switch (this) {
        ProfileSection.student => '学生资料',
        ProfileSection.ai => 'AI 配置',
        ProfileSection.device => '设备名称',
        ProfileSection.data => '数据管理',
        ProfileSection.about => '关于',
      };

  String get description => switch (this) {
        ProfileSection.student => '昵称、教育阶段和入学年份',
        ProfileSection.ai => '统一管理 API 地址、密钥和模型',
        ProfileSection.device => '附近设备和配对时显示的名称',
        ProfileSection.data => '本地缓存和应用偏好',
        ProfileSection.about => '版本、隐私、许可和图标来源',
      };

  IconData get icon => switch (this) {
        ProfileSection.student => Icons.school_outlined,
        ProfileSection.ai => Icons.auto_awesome_outlined,
        ProfileSection.device => Icons.devices_outlined,
        ProfileSection.data => Icons.storage_outlined,
        ProfileSection.about => Icons.info_outline,
      };
}
