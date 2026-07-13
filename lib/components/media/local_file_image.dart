import 'dart:io';

import 'package:flutter/material.dart';

class LocalFileImage extends StatelessWidget {
  const LocalFileImage({
    required this.path,
    this.height,
    this.fit = BoxFit.cover,
    super.key,
  });

  final String path;
  final double? height;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    return Image.file(File(path), height: height, fit: fit);
  }
}
