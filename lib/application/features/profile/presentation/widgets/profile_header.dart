import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/media/app_avatar.dart';
import 'package:bandu_wrong_notebook/components/media/local_file_image.dart';
import 'package:flutter/material.dart';

class ProfileHeader extends StatelessWidget {
  const ProfileHeader({
    required this.summary,
    required this.modelConfigLabel,
    required this.deviceNameLabel,
    required this.avatar,
    required this.onTapAvatar,
    super.key,
  });

  final StudentProfileSummary summary;
  final String modelConfigLabel;
  final String deviceNameLabel;
  final AvatarSettings avatar;
  final VoidCallback onTapAvatar;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final hasWallpaper = avatar.wallpaperPath != null;
    final foregroundColor = hasWallpaper ? Colors.white : colorScheme.onSurface;
    final secondaryColor = hasWallpaper
        ? Colors.white.withValues(alpha: 0.82)
        : colorScheme.onSurfaceVariant;

    return Stack(
      fit: StackFit.expand,
      children: [
        if (hasWallpaper)
          LocalFileImage(
            path: avatar.wallpaperPath!,
            width: double.infinity,
            height: double.infinity,
            borderRadius: 0,
          )
        else
          ColoredBox(color: colorScheme.surfaceContainerHigh),
        if (hasWallpaper)
          ColoredBox(color: Colors.black.withValues(alpha: 0.38)),
        Align(
          alignment: Alignment.bottomLeft,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.page,
              72,
              AppSpacing.page,
              AppSpacing.xLarge,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Semantics(
                  button: true,
                  label: '设置头像',
                  child: InkWell(
                    customBorder: const CircleBorder(),
                    onTap: onTapAvatar,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: hasWallpaper
                              ? Colors.white
                              : colorScheme.outlineVariant,
                          width: 2,
                        ),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(3),
                        child: AppAvatar(
                          initial: summary.initial,
                          size: 88,
                          backgroundColor: Color(avatar.colorValue),
                          imagePath: avatar.imagePath,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.large),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.small),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          summary.nickname,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context)
                              .textTheme
                              .headlineMedium
                              ?.copyWith(
                                color: foregroundColor,
                                fontWeight: FontWeight.w700,
                              ),
                        ),
                        const SizedBox(height: AppSpacing.xSmall),
                        Text(
                          summary.headline,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style:
                              Theme.of(context).textTheme.bodyLarge?.copyWith(
                                    color: secondaryColor,
                                  ),
                        ),
                        const SizedBox(height: AppSpacing.small),
                        _SummaryLine(
                          icon: Icons.auto_awesome_outlined,
                          text: '模型配置：$modelConfigLabel',
                          color: secondaryColor,
                        ),
                        const SizedBox(height: AppSpacing.xSmall),
                        _SummaryLine(
                          icon: Icons.devices_outlined,
                          text: '设备名称：$deviceNameLabel',
                          color: secondaryColor,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _SummaryLine extends StatelessWidget {
  const _SummaryLine({
    required this.icon,
    required this.text,
    required this.color,
  });

  final IconData icon;
  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: AppSizes.iconSmall, color: color),
        const SizedBox(width: AppSpacing.small),
        Expanded(
          child: Text(
            text,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: color,
                ),
          ),
        ),
      ],
    );
  }
}
