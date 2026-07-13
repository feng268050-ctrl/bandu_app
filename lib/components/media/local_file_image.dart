import 'dart:io';

import 'package:bandu_wrong_notebook/components/design_system/tokens/app_radius.dart';
import 'package:flutter/material.dart';

class LocalFileImage extends StatelessWidget {
  const LocalFileImage({
    required this.path,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    this.borderRadius = AppRadius.small,
    super.key,
  });

  final String path;
  final double? width;
  final double? height;
  final BoxFit fit;
  final double borderRadius;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: Image.file(
        File(path),
        width: width,
        height: height,
        fit: fit,
        errorBuilder: (context, error, stackTrace) {
          return ColoredBox(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            child: const Center(child: Icon(Icons.broken_image_outlined)),
          );
        },
      ),
    );
  }
}
