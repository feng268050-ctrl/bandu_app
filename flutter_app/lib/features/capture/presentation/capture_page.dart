import 'dart:io';

import 'package:bandu_wrong_notebook/features/capture/domain/capture_models.dart';
import 'package:bandu_wrong_notebook/features/capture/presentation/capture_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class CapturePage extends ConsumerWidget {
  const CapturePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(captureControllerProvider);
    final controller = ref.read(captureControllerProvider.notifier);
    final isBusy =
        state.phase == CapturePhase.capturing ||
        state.phase == CapturePhase.uploading ||
        state.phase == CapturePhase.analyzing;

    return Scaffold(
      appBar: AppBar(title: const Text('拍题')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (state.localImagePath != null)
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.file(
                File(state.localImagePath!),
                height: 320,
                fit: BoxFit.cover,
              ),
            )
          else
            AspectRatio(
              aspectRatio: 4 / 3,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: Theme.of(context).colorScheme.outlineVariant,
                  ),
                ),
                child: const Center(child: Icon(Icons.add_a_photo_outlined)),
              ),
            ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: isBusy ? null : controller.takePhoto,
                  icon: const Icon(Icons.photo_camera_outlined),
                  label: const Text('拍照'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isBusy ? null : controller.pickFromGallery,
                  icon: const Icon(Icons.photo_library_outlined),
                  label: const Text('相册'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: state.canAnalyze && !isBusy ? controller.analyze : null,
            icon: isBusy
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.auto_awesome),
            label: const Text('AI 分析'),
          ),
          if (state.errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(
              state.errorMessage!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          if (state.result != null) ...[
            const SizedBox(height: 24),
            _AnalyzeResultView(result: state.result!),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: state.canSave && !isBusy
                  ? controller.saveToLibrary
                  : null,
              icon: state.savedErrorItemId == null
                  ? const Icon(Icons.save_outlined)
                  : const Icon(Icons.check_circle_outline),
              label: Text(
                state.savedErrorItemId == null ? '保存到错题本' : '已保存到错题本',
              ),
            ),
            if (state.savedErrorItemId != null) ...[
              const SizedBox(height: 8),
              TextButton.icon(
                onPressed: controller.reset,
                icon: const Icon(Icons.add_a_photo_outlined),
                label: const Text('继续拍题'),
              ),
            ],
          ],
        ],
      ),
    );
  }
}

class _AnalyzeResultView extends StatelessWidget {
  const _AnalyzeResultView({required this.result});

  final AnalyzeResult result;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(result.title, style: Theme.of(context).textTheme.titleMedium),
            if (result.questionText != null) ...[
              const SizedBox(height: 12),
              Text(result.questionText!),
            ],
            if (result.answer != null) ...[
              const SizedBox(height: 12),
              Text('答案', style: Theme.of(context).textTheme.labelLarge),
              Text(result.answer!),
            ],
            if (result.analysis != null) ...[
              const SizedBox(height: 12),
              Text('解析', style: Theme.of(context).textTheme.labelLarge),
              Text(result.analysis!),
            ],
          ],
        ),
      ),
    );
  }
}
