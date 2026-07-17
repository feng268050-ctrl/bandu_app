import 'package:bandu_wrong_notebook/application/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/capture_controller.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/widgets/analyze_result_card.dart';
import 'package:bandu_wrong_notebook/application/features/capture/presentation/widgets/capture_image_selector.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/actions/app_default_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
            onTakePhoto: () => controller.takePhoto(),
            onPickFromGallery: () => controller.pickFromGallery(),
            onImportPdf: () => context.go('/capture/pdf-import'),
          ),
          const SizedBox(height: AppSpacing.medium),
          AppAsyncPrimaryButton(
            label: 'AI 分析',
            expanded: true,
            isLoading: state.phase == CapturePhase.analyzing,
            onPressed: state.canAnalyze ? controller.analyze : null,
            icon: const Icon(Icons.auto_awesome),
          ),
          if (isBusy) ...[
            const SizedBox(height: AppSpacing.small),
            AppDefaultButton(
              label: '取消当前任务',
              expanded: true,
              onPressed: controller.cancelOngoingRequest,
              icon: const Icon(Icons.close),
            ),
          ],
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
          if (state.noticeMessage != null) ...[
            const SizedBox(height: AppSpacing.medium),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.info_outline, color: colorScheme.primary),
                const SizedBox(width: AppSpacing.small),
                Expanded(child: Text(state.noticeMessage!)),
              ],
            ),
          ],
          if (state.result != null) ...[
            const SizedBox(height: AppSpacing.xLarge),
            if (state.result!.resolvedModel != null)
              Card.outlined(
                child: ListTile(
                  leading: const Icon(Icons.auto_awesome_outlined),
                  title: Text(
                    state.result!.resolvedModel!.fallbackOccurred
                        ? '主模型不可用，已切换到备用模型'
                        : '由 Auto 选择：${state.result!.resolvedModel!.displayName}',
                  ),
                  subtitle: state.result!.resolvedModel!.fallbackOccurred
                      ? Text(state.result!.resolvedModel!.displayName)
                      : null,
                ),
              ),
            AnalyzeResultCard(result: state.result!),
            const SizedBox(height: AppSpacing.medium),
            AppAsyncPrimaryButton(
              label: state.savedErrorItemId != null
                  ? '已保存'
                  : state.queuedTaskId != null
                      ? '等待上传'
                      : state.phase == CapturePhase.uploading
                          ? '正在保存'
                          : '保存到错题本',
              expanded: true,
              isLoading: state.phase == CapturePhase.uploading,
              onPressed:
                  state.canSave && !isBusy ? controller.saveToLibrary : null,
              icon: Icon(
                state.savedErrorItemId == null
                    ? state.queuedTaskId == null
                        ? Icons.save_outlined
                        : Icons.cloud_upload_outlined
                    : Icons.check,
              ),
            ),
            if (state.savedErrorItemId != null ||
                state.queuedTaskId != null) ...[
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
