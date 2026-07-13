import 'package:bandu_wrong_notebook/application/features/profile/domain/avatar_settings.dart';
import 'package:bandu_wrong_notebook/application/features/profile/domain/profile_models.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/media/app_avatar.dart';
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
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.small),
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
                size: AppSizes.avatarLarge,
                backgroundColor: Color(avatar.colorValue),
                imagePath: avatar.imagePath,
              ),
            ),
          ),
          const SizedBox(width: AppSpacing.large),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  summary.nickname,
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: AppSpacing.xSmall),
                Text(
                  summary.headline,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                ),
                const SizedBox(height: AppSpacing.small),
                _SummaryLine(
                  icon: Icons.auto_awesome_outlined,
                  text: '模型配置：$modelConfigLabel',
                ),
                const SizedBox(height: AppSpacing.xSmall),
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
    final color = Theme.of(context).colorScheme.onSurfaceVariant;
    return Row(
      children: [
        Icon(icon, size: AppSizes.iconSmall, color: color),
        const SizedBox(width: AppSpacing.small),
        Expanded(
          child: Text(
            text,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: color),
          ),
        ),
      ],
    );
  }
}
