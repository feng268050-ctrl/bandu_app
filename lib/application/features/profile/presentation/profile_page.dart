import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/profile_section_page.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/student_profile_editor_page.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/avatar_editor_dialog.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_header.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_section_tile.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/components/actions/app_secondary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  ProfileSection? _currentSection;

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final bypassAuth = ref.watch(authBypassProvider);
    final user = authState.user;
    final deviceName = ref.watch(deviceNameProvider).valueOrNull ?? '读取中';
    final overview = ProfileOverviewState.fromUser(
      user,
      deviceNameLabel: deviceName,
    );

    if (_currentSection == ProfileSection.student) {
      return StudentProfileEditorPage(
        user: user,
        onBack: () => setState(() => _currentSection = null),
      );
    }
    if (_currentSection != null) {
      return ProfileSectionPage(
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
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          ProfileHeader(
            summary: overview.summary,
            modelConfigLabel: overview.modelConfigLabel,
            deviceNameLabel: overview.deviceNameLabel,
            avatar: avatar,
            onTapAvatar: () => _showAvatarEditor(avatar, overview.summary),
          ),
          const SizedBox(height: AppSpacing.medium),
          const Divider(),
          const SizedBox(height: AppSpacing.medium),
          for (final section in overview.sections) ...[
            ProfileSectionTile(
              section: section,
              onTap: () => setState(() => _currentSection = section),
            ),
            const SizedBox(height: AppSpacing.small),
          ],
          if (!bypassAuth) ...[
            const SizedBox(height: AppSpacing.small),
            AppSecondaryButton(
              label: '退出登录',
              expanded: true,
              onPressed: authState.isBusy ? null : _logout,
              icon: const Icon(Icons.logout),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _showAvatarEditor(
    AvatarSettings current,
    StudentProfileSummary summary,
  ) async {
    await showAvatarEditorDialog(
      context: context,
      current: current,
      initial: summary.initial,
      onPickImage: _pickAvatarImage,
      onSave: ({required colorValue, imagePath}) async {
        final controller = ref.read(avatarSettingsProvider.notifier);
        if (imagePath != null) {
          await controller.saveImage(
            sourcePath: imagePath,
            fallbackColorValue: colorValue,
          );
        } else {
          await controller.saveColor(colorValue);
        }
      },
    );
  }

  Future<String?> _pickAvatarImage() async {
    final source = await showModalBottomSheet<AvatarImageSource>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt_outlined),
              title: const Text('拍照'),
              onTap: () => Navigator.of(context).pop(AvatarImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('从相册选择'),
              onTap: () => Navigator.of(context).pop(AvatarImageSource.gallery),
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
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '清除本地数据',
      message: '将清除本机错题缓存和头像配置，服务端错题不会删除。',
      confirmLabel: '清除',
      isDestructive: true,
    );
    if (!confirmed) {
      return;
    }
    await ref.read(clearLocalDataUseCaseProvider).call();
    ref.invalidate(avatarSettingsProvider);
    ref.invalidate(libraryControllerProvider);
    if (mounted) {
      showAppSuccessSnackBar(context, '本地数据已清除');
    }
  }

  Future<void> _logout() async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '退出登录',
      message: '确定退出当前账号吗？',
      confirmLabel: '退出',
    );
    if (confirmed) {
      await ref.read(authControllerProvider.notifier).logout();
    }
  }
}
