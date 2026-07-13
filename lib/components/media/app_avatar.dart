import 'dart:io';

import 'package:flutter/material.dart';

class AppAvatar extends StatelessWidget {
  const AppAvatar({
    required this.initial,
    required this.color,
    required this.radius,
    this.imagePath,
    super.key,
  });

  final String initial;
  final Color color;
  final double radius;
  final String? imagePath;

  @override
  Widget build(BuildContext context) {
    final file = imagePath == null ? null : File(imagePath!);
    final hasImage = file?.existsSync() == true;

    return CircleAvatar(
      radius: radius,
      backgroundColor: color,
      foregroundImage: hasImage ? FileImage(file!) : null,
      child: hasImage
          ? null
          : Text(
              initial,
              style: TextStyle(
                color: Colors.white,
                fontSize: radius * 0.58,
                fontWeight: FontWeight.w700,
              ),
            ),
    );
  }
}
