import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/capture_controller.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/widgets/analyze_result_card.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/widgets/capture_image_selector.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class CapturePage extends ConsumerWidget {
  const CapturePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(captureControllerProvider);
    final controller = ref.read(captureControllerProvider.notifier);
    final isBusy = state.phase == CapturePhase.capturing ||
        state.phase == CapturePhase.uploading ||
        state.phase == CapturePhase.analyzing;
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('拍题')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.page),
        children: [
          CaptureImageSelector(
            imagePath: state.localImagePath,
            isBusy: isBusy,
            onTakePhoto: controller.takePhoto,
            onPickFromGallery: controller.pickFromGallery,
          ),
          const SizedBox(height: AppSpacing.medium),
          AppAsyncPrimaryButton(
            label: 'AI 分析',
            expanded: true,
            isLoading: isBusy,
            onPressed: state.canAnalyze ? controller.analyze : null,
            icon: const Icon(Icons.auto_awesome),
          ),
          if (state.errorMessage != null) ...[
            const SizedBox(height: AppSpacing.medium),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.error_outline, color: colorScheme.error),
                const SizedBox(width: AppSpacing.small),
                Expanded(
                  child: Text(
                    state.errorMessage!,
                    style: Theme.of(
                      context,
                    ).textTheme.bodyMedium?.copyWith(color: colorScheme.error),
                  ),
                ),
              ],
            ),
          ],
          if (state.result != null) ...[
            const SizedBox(height: AppSpacing.xLarge),
            AnalyzeResultCard(result: state.result!),
            const SizedBox(height: AppSpacing.medium),
            AppPrimaryButton(
              label: state.savedErrorItemId == null ? '保存到错题本' : '已保存',
              expanded: true,
              onPressed:
                  state.canSave && !isBusy ? controller.saveToLibrary : null,
              icon: Icon(
                state.savedErrorItemId == null
                    ? Icons.save_outlined
                    : Icons.check,
              ),
            ),
            if (state.savedErrorItemId != null) ...[
              const SizedBox(height: AppSpacing.small),
              AppDefaultButton(
                label: '继续拍题',
                expanded: true,
                onPressed: controller.reset,
                icon: const Icon(Icons.camera_alt_outlined),
              ),
            ],
          ],
        ],
      ),
    );
  }
}
