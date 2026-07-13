import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/components/media/app_avatar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

extension on ProfileSection {
  String get title => switch (this) {
        ProfileSection.student => '学生资料',
        ProfileSection.ai => 'AI 配置',
        ProfileSection.device => '设备名称',
        ProfileSection.data => '数据管理',
        ProfileSection.about => '关于',
      };

  String get description => switch (this) {
        ProfileSection.student => '昵称、教育阶段和入学年份',
        ProfileSection.ai => '服务、模型、密钥和提示词',
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

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  static const _avatarColors = [
    Color(0xff202124),
    Color(0xff455a64),
    Color(0xff00695c),
    Color(0xff00897b),
    Color(0xff0277bd),
    Color(0xff1565c0),
    Color(0xff5e35b1),
    Color(0xff8e24aa),
    Color(0xffd81b60),
    Color(0xffc62828),
    Color(0xffe65100),
    Color(0xff6d4c41),
  ];

  ProfileSection? _currentSection;

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final user = authState.user;
    final deviceName = ref.watch(deviceNameProvider).valueOrNull ?? '读取中';
    final overview = ProfileOverviewState.fromUser(
      user,
      deviceNameLabel: deviceName,
    );

    if (_currentSection == ProfileSection.student) {
      return _StudentProfileEditorPage(
        user: user,
        onBack: () => setState(() => _currentSection = null),
      );
    }
    if (_currentSection != null) {
      return _ProfileSectionPage(
        section: _currentSection!,
        overview: overview,
        version: ref.watch(packageVersionProvider).valueOrNull ?? 'v0.1.1',
        onBack: () => setState(() => _currentSection = null),
        onClearLocalData: _clearLocalData,
      );
    }

    final avatar =
        ref.watch(avatarSettingsProvider).valueOrNull ?? const AvatarSettings();

    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StudentSummaryHeader(
            summary: overview.summary,
            modelConfigLabel: overview.modelConfigLabel,
            deviceNameLabel: overview.deviceNameLabel,
            avatar: avatar,
            onTapAvatar: () => _showAvatarEditor(avatar, overview.summary),
          ),
          const SizedBox(height: 12),
          Divider(color: Theme.of(context).colorScheme.outlineVariant),
          const SizedBox(height: 12),
          for (final section in overview.sections)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _ProfileSectionTile(
                section: section,
                onTap: () => setState(() => _currentSection = section),
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

  Future<void> _showAvatarEditor(
    AvatarSettings current,
    StudentProfileSummary summary,
  ) async {
    var draftColor = Color(current.colorValue);
    var draftImagePath = current.imagePath;
    var saving = false;
    String? errorMessage;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final hasImage = draftImagePath != null;
            return AlertDialog(
              title: const Text('头像'),
              content: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 360),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    AppAvatar(
                      initial: summary.initial,
                      radius: 48,
                      color: draftColor,
                      imagePath: draftImagePath,
                    ),
                    const SizedBox(height: 24),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: [
                          _AvatarAddChoice(
                            selected: hasImage,
                            onTap: saving
                                ? null
                                : () async {
                                    final path = await _pickAvatarImage(
                                      dialogContext,
                                    );
                                    if (path != null) {
                                      setDialogState(() {
                                        draftImagePath = path;
                                        errorMessage = null;
                                      });
                                    }
                                  },
                          ),
                          for (final color in _avatarColors)
                            _AvatarColorChoice(
                              color: color,
                              selected: !hasImage &&
                                  color.toARGB32() == draftColor.toARGB32(),
                              onTap: saving
                                  ? null
                                  : () => setDialogState(() {
                                        draftColor = color;
                                        draftImagePath = null;
                                        errorMessage = null;
                                      }),
                            ),
                        ],
                      ),
                    ),
                    if (errorMessage != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        errorMessage!,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: saving ? null : () => Navigator.of(context).pop(),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: saving
                      ? null
                      : () async {
                          setDialogState(() {
                            saving = true;
                            errorMessage = null;
                          });
                          try {
                            final controller = ref.read(
                              avatarSettingsProvider.notifier,
                            );
                            if (hasImage) {
                              await controller.saveImage(
                                sourcePath: draftImagePath!,
                                fallbackColorValue: draftColor.toARGB32(),
                              );
                            } else {
                              await controller.saveColor(draftColor.toARGB32());
                            }
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                          } catch (error) {
                            setDialogState(() {
                              saving = false;
                              errorMessage = error.toString();
                            });
                          }
                        },
                  child: const Text('确认'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<String?> _pickAvatarImage(BuildContext context) async {
    final source = await showModalBottomSheet<AvatarImageSource>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('拍照'),
              onTap: () => Navigator.of(
                context,
              ).pop(AvatarImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('从相册选择'),
              onTap: () => Navigator.of(
                context,
              ).pop(AvatarImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (source == null) {
      return null;
    }
    return ref.read(avatarSettingsProvider.notifier).pickImage(source);
  }

  Future<void> _clearLocalData() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('清除本地数据'),
        content: const Text('将清除本机错题缓存和头像配置，服务端错题不会删除。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('清除'),
          ),
        ],
      ),
    );
    if (confirmed != true) {
      return;
    }
    await ref.read(clearLocalDataUseCaseProvider).call();
    ref.invalidate(avatarSettingsProvider);
    ref.invalidate(libraryControllerProvider);
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('本地数据已清除')));
    }
  }
}

class _StudentSummaryHeader extends StatelessWidget {
  const _StudentSummaryHeader({
    required this.summary,
    required this.modelConfigLabel,
    required this.deviceNameLabel,
    required this.avatar,
    required this.onTapAvatar,
  });

  final StudentProfileSummary summary;
  final String modelConfigLabel;
  final String deviceNameLabel;
  final AvatarSettings avatar;
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
            label: '设置头像',
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onTapAvatar,
              child: AppAvatar(
                initial: summary.initial,
                radius: 36,
                color: Color(avatar.colorValue),
                imagePath: avatar.imagePath,
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
  const _SummaryLine({required this.icon, required this.text});

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
  const _ProfileSectionTile({required this.section, required this.onTap});

  final ProfileSection section;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        onTap: onTap,
        leading: Icon(section.icon),
        title: Text(section.title),
        subtitle: Text(section.description),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}

class _StudentProfileEditorPage extends ConsumerStatefulWidget {
  const _StudentProfileEditorPage({required this.user, required this.onBack});

  final UserProfile? user;
  final VoidCallback onBack;

  @override
  ConsumerState<_StudentProfileEditorPage> createState() =>
      _StudentProfileEditorPageState();
}

class _StudentProfileEditorPageState
    extends ConsumerState<_StudentProfileEditorPage> {
  late final TextEditingController _nameController;
  late final TextEditingController _yearController;
  late String _educationStage;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.user?.name);
    _yearController = TextEditingController(
      text: widget.user?.enrollmentYear?.toString() ?? '',
    );
    _educationStage = widget.user?.educationStage ?? 'primary';
  }

  @override
  void dispose() {
    _nameController.dispose();
    _yearController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('学生资料'),
        leading: IconButton(
          tooltip: '返回',
          onPressed: widget.onBack,
          icon: const Icon(Icons.arrow_back),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          TextField(
            controller: _nameController,
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(
              labelText: '昵称',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<String>(
            initialValue: _educationStage,
            decoration: const InputDecoration(
              labelText: '教育阶段',
              border: OutlineInputBorder(),
            ),
            items: const [
              DropdownMenuItem(value: 'primary', child: Text('小学')),
              DropdownMenuItem(value: 'junior_high', child: Text('初中')),
              DropdownMenuItem(value: 'senior_high', child: Text('高中')),
              DropdownMenuItem(value: 'university', child: Text('大学')),
            ],
            onChanged: authState.isBusy
                ? null
                : (value) => setState(
                      () => _educationStage = value ?? _educationStage,
                    ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _yearController,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: '入学年份',
              hintText: '例如 2022',
              border: OutlineInputBorder(),
            ),
          ),
          if (_errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(
              _errorMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: authState.isBusy ? null : _save,
            icon: authState.isBusy
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text('保存'),
          ),
        ],
      ),
    );
  }

  Future<void> _save() async {
    final name = _nameController.text.trim();
    final year = int.tryParse(_yearController.text.trim());
    final currentYear = DateTime.now().year;
    if (name.isEmpty) {
      setState(() => _errorMessage = '昵称不能为空');
      return;
    }
    if (year == null || year < 1900 || year > currentYear) {
      setState(() => _errorMessage = '请输入有效的入学年份');
      return;
    }

    final saved = await ref.read(authControllerProvider.notifier).updateProfile(
          name: name,
          educationStage: _educationStage,
          enrollmentYear: year,
        );
    if (!mounted) {
      return;
    }
    if (saved) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('学生资料已保存')));
      widget.onBack();
    } else {
      setState(() {
        _errorMessage = ref.read(authControllerProvider).errorMessage ?? '保存失败';
      });
    }
  }
}

class _ProfileSectionPage extends StatelessWidget {
  const _ProfileSectionPage({
    required this.section,
    required this.overview,
    required this.version,
    required this.onBack,
    required this.onClearLocalData,
  });

  final ProfileSection section;
  final ProfileOverviewState overview;
  final String version;
  final VoidCallback onBack;
  final Future<void> Function() onClearLocalData;

  @override
  Widget build(BuildContext context) {
    final List<_InfoRow> rows = switch (section) {
      ProfileSection.ai => const [
          _InfoRow('服务提供商', '由 bandu_web 托管'),
          _InfoRow('拍题分析模型', '服务器默认模型'),
          _InfoRow('API 密钥', '仅保存在服务端'),
        ],
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

    return Scaffold(
      appBar: AppBar(
        title: Text(section.title),
        leading: IconButton(
          tooltip: '返回',
          onPressed: onBack,
          icon: const Icon(Icons.arrow_back),
        ),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.all(20),
        itemCount: rows.length + (section == ProfileSection.data ? 1 : 0),
        separatorBuilder: (context, index) => const Divider(),
        itemBuilder: (context, index) {
          if (index == rows.length) {
            return Padding(
              padding: const EdgeInsets.only(top: 12),
              child: OutlinedButton.icon(
                onPressed: onClearLocalData,
                icon: const Icon(Icons.cleaning_services_outlined),
                label: const Text('清除本地数据'),
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
}

class _InfoRow {
  const _InfoRow(this.label, this.value);

  final String label;
  final String value;
}

class _AvatarAddChoice extends StatelessWidget {
  const _AvatarAddChoice({required this.selected, required this.onTap});

  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkResponse(
      radius: 26,
      onTap: onTap,
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(
            color: selected
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.outlineVariant,
            width: selected ? 3 : 1,
          ),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}

class _AvatarColorChoice extends StatelessWidget {
  const _AvatarColorChoice({
    required this.color,
    required this.selected,
    required this.onTap,
  });

  final Color color;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkResponse(
      radius: 26,
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
        child: selected ? const Icon(Icons.check, color: Colors.white) : null,
      ),
    );
  }
}
