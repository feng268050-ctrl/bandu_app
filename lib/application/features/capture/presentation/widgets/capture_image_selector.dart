import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/media/local_file_image.dart';
import 'package:flutter/material.dart';

class CaptureImageSelector extends StatelessWidget {
  const CaptureImageSelector({
    required this.imagePath,
    required this.isBusy,
    required this.onTakePhoto,
    required this.onPickFromGallery,
    required this.onImportPdf,
    super.key,
  });

  final String? imagePath;
  final bool isBusy;
  final VoidCallback onTakePhoto;
  final VoidCallback onPickFromGallery;
  final VoidCallback onImportPdf;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Card.outlined(
          child: InkWell(
            onTap: isBusy ? null : onTakePhoto,
            child: AspectRatio(
              aspectRatio: 4 / 3,
              child: imagePath == null
                  ? _EmptyImageSelector(isBusy: isBusy)
                  : LocalFileImage(
                      path: imagePath!,
                      width: double.infinity,
                      height: double.infinity,
                      borderRadius: 0,
                    ),
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.large),
        Row(
          children: [
            Expanded(
              child: AppPrimaryButton(
                label: '导入 PDF 题集',
                icon: const Icon(Icons.picture_as_pdf_outlined),
                onPressed: isBusy ? null : onImportPdf,
              ),
            ),
            const SizedBox(width: AppSpacing.medium),
            Expanded(
              child: AppDefaultButton(
                label: '相册',
                icon: const Icon(Icons.photo_library_outlined),
                onPressed: isBusy ? null : onPickFromGallery,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _EmptyImageSelector extends StatelessWidget {
  const _EmptyImageSelector({required this.isBusy});

  final bool isBusy;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(
          Icons.add_a_photo_outlined,
          size: 40,
          color: colorScheme.onSurfaceVariant,
        ),
        const SizedBox(height: AppSpacing.small),
        Text(
          isBusy ? '正在处理图片' : '点击拍摄题目',
          style: Theme.of(
            context,
          ).textTheme.bodyMedium?.copyWith(color: colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}
