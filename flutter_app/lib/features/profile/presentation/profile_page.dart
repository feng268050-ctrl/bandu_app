import 'package:bandu_wrong_notebook/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/features/profile/domain/profile_models.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  static const _avatarColors = [
    Color(0xff2563eb),
    Color(0xff16a34a),
    Color(0xffdc2626),
    Color(0xff9333ea),
    Color(0xff0891b2),
    Color(0xff4b5563),
  ];

  ProfileSection? _currentSection;
  int _avatarColorIndex = 0;

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final user = authState.user;
    final overview = ProfileOverviewState.fromUser(user);

    if (_currentSection != null) {
      return _ProfileSectionPage(
        section: _currentSection!,
        overview: overview,
        enrollmentYear: user?.enrollmentYear,
        onBack: () => setState(() => _currentSection = null),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StudentSummaryHeader(
            summary: overview.summary,
            modelConfigLabel: overview.modelConfigLabel,
            deviceNameLabel: overview.deviceNameLabel,
            avatarColor: _avatarColors[
                _avatarColorIndex.remainder(_avatarColors.length)],
            onTapAvatar: _showAvatarActions,
          ),
          const SizedBox(height: 12),
          Divider(color: Theme.of(context).colorScheme.outlineVariant),
          const SizedBox(height: 12),
          ...overview.sections.map(
            (section) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _ProfileSectionTile(
                section: section,
                onTap: () => setState(() => _currentSection = section),
              ),
            ),
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: authState.isBusy
                ? null
                : () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout),
            label: const Text('退出登录'),
          ),
        ],
      ),
    );
  }

  void _showAvatarActions() {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '头像',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 16),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.photo_library_outlined),
                  title: const Text('图片头像'),
                  onTap: () => Navigator.of(context).pop(),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    for (var index = 0; index < _avatarColors.length; index++)
                      _AvatarColorChoice(
                        color: _avatarColors[index],
                        selected: index == _avatarColorIndex,
                        onTap: () {
                          setState(() => _avatarColorIndex = index);
                          Navigator.of(context).pop();
                        },
                      ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _StudentSummaryHeader extends StatelessWidget {
  const _StudentSummaryHeader({
    required this.summary,
    required this.modelConfigLabel,
    required this.deviceNameLabel,
    required this.avatarColor,
    required this.onTapAvatar,
  });

  final StudentProfileSummary summary;
  final String modelConfigLabel;
  final String deviceNameLabel;
  final Color avatarColor;
  final VoidCallback onTapAvatar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Semantics(
            button: true,
            label: '头像',
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onTapAvatar,
              child: CircleAvatar(
                radius: 34,
                backgroundColor: avatarColor,
                child: Text(
                  summary.initial,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  summary.nickname,
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 4),
                Text(
                  summary.headline,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                ),
                const SizedBox(height: 8),
                _SummaryLine(
                  icon: Icons.auto_awesome_outlined,
                  text: '模型配置：$modelConfigLabel',
                ),
                const SizedBox(height: 4),
                _SummaryLine(
                  icon: Icons.devices_outlined,
                  text: '设备名称：$deviceNameLabel',
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryLine extends StatelessWidget {
  const _SummaryLine({
    required this.icon,
    required this.text,
  });

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(
          icon,
          size: 16,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            text,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
          ),
        ),
      ],
    );
  }
}

class _ProfileSectionTile extends StatelessWidget {
  const _ProfileSectionTile({
    required this.section,
    required this.onTap,
  });

  final ProfileSection section;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(section.icon),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      section.title,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      section.description,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color:
                                Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}

class _ProfileSectionPage extends StatelessWidget {
  const _ProfileSectionPage({
    required this.section,
    required this.overview,
    required this.enrollmentYear,
    required this.onBack,
  });

  final ProfileSection section;
  final ProfileOverviewState overview;
  final int? enrollmentYear;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final rows = _sectionRows(section);

    return Scaffold(
      appBar: AppBar(
        title: Text(section.title),
        leading: IconButton(
          tooltip: '返回',
          onPressed: onBack,
          icon: const Icon(Icons.arrow_back),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(section.icon),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          section.title,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 8),
                        Text(section.description),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          for (final row in rows)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: ListTile(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                  side: BorderSide(
                    color: Theme.of(context).colorScheme.outlineVariant,
                  ),
                ),
                leading: Icon(row.icon),
                title: Text(row.title),
                subtitle: row.subtitle == null ? null : Text(row.subtitle!),
                trailing: row.trailing == null ? null : Text(row.trailing!),
              ),
            ),
        ],
      ),
    );
  }

  List<_ProfileSectionRow> _sectionRows(ProfileSection section) {
    return switch (section) {
      ProfileSection.student => [
          _ProfileSectionRow(
            icon: Icons.badge_outlined,
            title: '昵称',
            trailing: overview.summary.nickname,
          ),
          _ProfileSectionRow(
            icon: Icons.school_outlined,
            title: '教育阶段',
            trailing: overview.summary.educationStageLabel ?? '未设置',
          ),
          _ProfileSectionRow(
            icon: Icons.event_outlined,
            title: '入学年份',
            trailing: enrollmentYear?.toString() ?? '未设置',
          ),
        ],
      ProfileSection.ai => const [
          _ProfileSectionRow(
            icon: Icons.cloud_outlined,
            title: '服务提供商',
            trailing: '后端托管',
          ),
          _ProfileSectionRow(
            icon: Icons.image_search_outlined,
            title: '拍题分析模型',
            trailing: '服务器默认',
          ),
          _ProfileSectionRow(
            icon: Icons.chat_outlined,
            title: 'AI 辅导模型',
            trailing: '服务器默认',
          ),
          _ProfileSectionRow(
            icon: Icons.article_outlined,
            title: '提示词',
            trailing: '服务器端配置',
          ),
        ],
      ProfileSection.device => const [
          _ProfileSectionRow(
            icon: Icons.drive_file_rename_outline,
            title: '设备名称',
            trailing: '未设置',
          ),
          _ProfileSectionRow(
            icon: Icons.sync_alt_outlined,
            title: '附近传输显示名称',
            trailing: '未设置',
          ),
        ],
      ProfileSection.data => const [
          _ProfileSectionRow(
            icon: Icons.cleaning_services_outlined,
            title: '清除学习数据',
            subtitle: '错题、练习记录和本地缓存',
          ),
          _ProfileSectionRow(
            icon: Icons.restart_alt_outlined,
            title: '恢复出厂设置',
            subtitle: '资料、AI 配置和设备身份',
          ),
        ],
      ProfileSection.about => const [
          _ProfileSectionRow(
            icon: Icons.apps_outlined,
            title: '应用名称',
            trailing: '伴读题集',
          ),
          _ProfileSectionRow(
            icon: Icons.new_releases_outlined,
            title: '版本',
            trailing: '开发版本',
          ),
          _ProfileSectionRow(
            icon: Icons.privacy_tip_outlined,
            title: '隐私',
            subtitle: '学习数据默认保存在本机',
          ),
          _ProfileSectionRow(
            icon: Icons.code_outlined,
            title: '开源许可',
            subtitle: 'Flutter、Dart、Riverpod、Dio、go_router',
          ),
        ],
    };
  }
}

class _ProfileSectionRow {
  const _ProfileSectionRow({
    required this.icon,
    required this.title,
    this.subtitle,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final String? subtitle;
  final String? trailing;
}

class _AvatarColorChoice extends StatelessWidget {
  const _AvatarColorChoice({
    required this.color,
    required this.selected,
    required this.onTap,
  });

  final Color color;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkResponse(
      radius: 28,
      onTap: onTap,
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          border: Border.all(
            color: selected
                ? Theme.of(context).colorScheme.onSurface
                : Colors.transparent,
            width: 3,
          ),
        ),
        child: selected
            ? const Icon(
                Icons.check,
                color: Colors.white,
              )
            : null,
      ),
    );
  }
}
