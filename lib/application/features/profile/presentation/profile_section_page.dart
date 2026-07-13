import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_section_ui.dart';
import 'package:bandu_wrong_notebook/components/actions/app_secondary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class ProfileSectionPage extends StatelessWidget {
  const ProfileSectionPage({
    required this.section,
    required this.overview,
    required this.version,
    required this.onBack,
    required this.onClearLocalData,
    super.key,
  });

  final ProfileSection section;
  final ProfileOverviewState overview;
  final String version;
  final VoidCallback onBack;
  final Future<void> Function() onClearLocalData;

  @override
  Widget build(BuildContext context) {
    final rows = _rowsForSection();
    return Scaffold(
      appBar: AppBar(
        title: Text(section.title),
        leading: BackButton(onPressed: onBack),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.all(AppSpacing.page),
        itemCount: rows.length + (section == ProfileSection.data ? 1 : 0),
        separatorBuilder: (context, index) => const Divider(),
        itemBuilder: (context, index) {
          if (index == rows.length) {
            return Padding(
              padding: const EdgeInsets.only(top: AppSpacing.medium),
              child: AppSecondaryButton(
                label: '清除本地数据',
                expanded: true,
                onPressed: onClearLocalData,
                icon: const Icon(Icons.cleaning_services_outlined),
              ),
            );
          }
          final row = rows[index];
          return ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(row.label),
            subtitle: Text(row.value),
          );
        },
      ),
    );
  }

  List<_InfoRow> _rowsForSection() {
    return switch (section) {
      ProfileSection.ai => const [],
      ProfileSection.device => [
          _InfoRow('设备名称', overview.deviceNameLabel),
          const _InfoRow('数据连接', '通过 HTTPS Mobile API'),
        ],
      ProfileSection.data => const [
          _InfoRow('本地缓存', '错题摘要、错题详情和头像配置'),
          _InfoRow('服务端数据', '清理本地缓存不会删除服务端数据'),
        ],
      ProfileSection.about => [
          const _InfoRow('应用名称', '伴读题集'),
          _InfoRow('版本', version),
          const _InfoRow('隐私', '题目仅在确认后发送到所选服务端'),
          const _InfoRow('开源许可', 'Flutter、Riverpod、Dio、go_router'),
        ],
      ProfileSection.student => const [],
    };
  }
}

class _InfoRow {
  const _InfoRow(this.label, this.value);

  final String label;
  final String value;
}
