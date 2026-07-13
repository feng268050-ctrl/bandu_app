import 'dart:io';

import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:flutter/material.dart';

class AppAvatar extends StatelessWidget {
  const AppAvatar({
    required this.initial,
    this.size = AppSizes.avatarMedium,
    this.backgroundColor,
    this.imagePath,
    this.imageUrl,
    super.key,
  });

  final String initial;
  final double size;
  final Color? backgroundColor;
  final String? imagePath;
  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    final file = imagePath == null ? null : File(imagePath!);
    final hasImage = file?.existsSync() == true;
    final imageProvider = hasImage
        ? FileImage(file!) as ImageProvider
        : imageUrl?.trim().isNotEmpty == true
            ? NetworkImage(imageUrl!)
            : null;
    final colorScheme = Theme.of(context).colorScheme;
    final fallbackTextColor = backgroundColor == null
        ? colorScheme.onPrimaryContainer
        : ThemeData.estimateBrightnessForColor(backgroundColor!) ==
                Brightness.dark
            ? Colors.white
            : Colors.black;

    return CircleAvatar(
      radius: size / 2,
      backgroundColor: backgroundColor ?? colorScheme.primaryContainer,
      foregroundImage: imageProvider,
      onForegroundImageError: imageProvider == null ? null : (_, __) {},
      child: Text(
        initial,
        style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: fallbackTextColor,
              fontWeight: FontWeight.w700,
            ),
      ),
    );
  }
}
