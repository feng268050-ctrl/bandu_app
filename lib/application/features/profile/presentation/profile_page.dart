import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_controller.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_page.dart';
import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/auth/auth_providers.dart';
import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/profile_section_page.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/student_profile_editor_page.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/avatar_editor_dialog.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_header.dart';
import 'package:bandu_wrong_notebook/application/features/profile/presentation/widgets/profile_section_tile.dart';
import 'package:bandu_wrong_notebook/application/features/profile/profile_providers.dart';
import 'package:bandu_wrong_notebook/application/features/settings/presentation/app_settings_page.dart';
import 'package:bandu_wrong_notebook/application/features/settings/settings_providers.dart';
import 'package:bandu_wrong_notebook/application/features/tutor/presentation/tutor_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_secondary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({this.section, super.key});

  final ProfileSection? section;

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final bypassAuth = ref.watch(authBypassProvider);
    final user = authState.user;
    final deviceName = ref.watch(deviceNameProvider).valueOrNull ?? '读取中';
    final aiConfigs = ref.watch(aiConfigControllerProvider);
    final modelConfigLabel = aiConfigs.when(
      data: (configs) => configs.isEmpty ? '未配置' : '已配置 ${configs.length} 个模型',
      loading: () => '读取中',
      error: (_, __) => '读取失败',
    );
    final overview = ProfileOverviewState.fromUser(
      user,
      deviceNameLabel: deviceName,
      modelConfigLabel: modelConfigLabel,
    );

    if (widget.section == ProfileSection.student) {
      return StudentProfileEditorPage(
        user: user,
        onBack: () => context.pop(),
      );
    }
    if (widget.section == ProfileSection.ai) {
      return AiConfigPage(
        onBack: () => context.pop(),
        onChanged: ref.read(tutorControllerProvider.notifier).refreshModels,
      );
    }
    if (widget.section == ProfileSection.settings) {
      return AppSettingsPage(
        onBack: () => context.pop(),
        onOpenAiConfig: () => context.push('/profile/settings/ai'),
        onOpenDevice: () => context.push('/profile/settings/device'),
        onOpenData: () => context.push('/profile/settings/data'),
        modelConfigLabel: overview.modelConfigLabel,
        deviceNameLabel: overview.deviceNameLabel,
      );
    }
    if (widget.section != null) {
      return ProfileSectionPage(
        section: widget.section!,
        overview: overview,
        version: ref.watch(packageVersionProvider).valueOrNull ?? 'v0.1.1',
        onBack: () => context.pop(),
        onClearLocalData: _clearLocalData,
      );
    }

    final avatar =
        ref.watch(avatarSettingsProvider).valueOrNull ?? const AvatarSettings();
    final colorScheme = Theme.of(context).colorScheme;
    final hasWallpaper = avatar.wallpaperPath != null;
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(
          parent: AlwaysScrollableScrollPhysics(),
        ),
        slivers: [
          SliverAppBar(
            automaticallyImplyLeading: false,
            expandedHeight: 320,
            stretch: true,
            pinned: false,
            backgroundColor: colorScheme.surfaceContainerHigh,
            foregroundColor:
                hasWallpaper ? colorScheme.onPrimary : colorScheme.onSurface,
            surfaceTintColor: colorScheme.surfaceTint.withValues(alpha: 0),
            title: const Text('我的'),
            actions: [
              IconButton(
                tooltip: '设置壁纸',
                style: hasWallpaper
                    ? IconButton.styleFrom(
                        backgroundColor: colorScheme.shadow.withValues(
                          alpha: 0.32,
                        ),
                        foregroundColor: colorScheme.onPrimary,
                      )
                    : null,
                onPressed: () => _showWallpaperActions(avatar),
                icon: const Icon(Icons.wallpaper_outlined),
              ),
              const SizedBox(width: AppSpacing.small),
            ],
            shape: Border(
              bottom: BorderSide(color: colorScheme.outlineVariant),
            ),
            flexibleSpace: FlexibleSpaceBar(
              collapseMode: CollapseMode.parallax,
              stretchModes: const [StretchMode.zoomBackground],
              background: ProfileHeader(
                summary: overview.summary,
                modelConfigLabel: overview.modelConfigLabel,
                deviceNameLabel: overview.deviceNameLabel,
                avatar: avatar,
                onTapAvatar: () => _showAvatarEditor(avatar, overview.summary),
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.page,
              AppSpacing.large,
              AppSpacing.page,
              AppSpacing.xLarge,
            ),
            sliver: SliverList.list(
              children: [
                for (final section in overview.sections) ...[
                  ProfileSectionTile(
                    section: section,
                    onTap: () => context.push(_profileSectionLocation(section)),
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
          ),
        ],
      ),
    );
  }

  Future<void> _showWallpaperActions(AvatarSettings current) async {
    final action = await showModalBottomSheet<_WallpaperAction>(
      context: context,
      useRootNavigator: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: Text(
                current.wallpaperPath == null ? '选择壁纸' : '更换壁纸',
              ),
              onTap: () => Navigator.of(context).pop(_WallpaperAction.choose),
            ),
            if (current.wallpaperPath != null)
              ListTile(
                leading: const Icon(Icons.delete_outline),
                title: const Text('移除壁纸'),
                onTap: () => Navigator.of(context).pop(_WallpaperAction.remove),
              ),
          ],
        ),
      ),
    );
    if (action == null || !mounted) return;
    final controller = ref.read(avatarSettingsProvider.notifier);
    try {
      switch (action) {
        case _WallpaperAction.choose:
          final path = await controller.pickWallpaper();
          if (path != null) {
            await controller.saveWallpaper(path);
          }
        case _WallpaperAction.remove:
          await controller.removeWallpaper();
      }
    } catch (error) {
      if (mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
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
      useRootNavigator: true,
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
      message: '将清除本机缓存、头像、壁纸及临时图片，服务端错题不会删除。',
      confirmLabel: '清除',
      isDestructive: true,
    );
    if (!confirmed) {
      return;
    }
    await ref.read(clearLocalDataUseCaseProvider).call();
    ref.invalidate(avatarSettingsProvider);
    ref.invalidate(appPreferencesControllerProvider);
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

enum _WallpaperAction { choose, remove }

String _profileSectionLocation(ProfileSection section) {
  return switch (section) {
    ProfileSection.student => '/profile/student',
    ProfileSection.settings => '/profile/settings',
    ProfileSection.about => '/profile/about',
    ProfileSection.ai => '/profile/settings/ai',
    ProfileSection.device => '/profile/settings/device',
    ProfileSection.data => '/profile/settings/data',
    ProfileSection.pendingTasks ||
    ProfileSection.network =>
      throw UnsupportedError('该功能不在当前个人中心中展示'),
  };
}
