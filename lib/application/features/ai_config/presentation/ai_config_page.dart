import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_editor_page.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_model_controller.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_error_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_loading_view.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AiConfigPage extends ConsumerStatefulWidget {
  const AiConfigPage({
    required this.onBack,
    required this.onChanged,
    super.key,
  });

  final VoidCallback onBack;
  final Future<void> Function() onChanged;

  @override
  ConsumerState<AiConfigPage> createState() => _AiConfigPageState();
}

class _AiConfigPageState extends ConsumerState<AiConfigPage> {
  AiModelSummary? _editingModel;

  @override
  Widget build(BuildContext context) {
    if (_editingModel != null) {
      return AiConfigEditorPage(
        model: _editingModel!,
        onBack: () => setState(() => _editingModel = null),
        onSave: (draft) async {
          await ref.read(aiModelCatalogControllerProvider.notifier).save(
                draft,
                id: _editingModel?.id,
              );
          await widget.onChanged();
          if (mounted) setState(() => _editingModel = null);
        },
      );
    }
    final catalog = ref.watch(aiModelCatalogControllerProvider);
    final preferences = ref.watch(aiPurposePreferencesControllerProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI 模型'),
        leading: BackButton(onPressed: widget.onBack),
        actions: [
          IconButton(
            tooltip: '添加模型',
            onPressed: () => setState(() => _editingModel = const AiModelSummary(
              id: '',
              displayName: '',
              provider: '',
              modelName: '',
              kind: AiModelKind.user,
              enabled: true,
              participatesInAuto: true,
              capabilities: AiModelCapabilities(),
            )),
            icon: const Icon(Icons.add),
          ),
        ],
      ),
      body: catalog.when(
        loading: () => const AppLoadingView(message: '正在读取 AI 模型'),
        error: (error, _) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: ref.read(aiModelCatalogControllerProvider.notifier).refresh,
        ),
        data: (items) => RefreshIndicator(
          onRefresh: () async {
            await ref.read(aiModelCatalogControllerProvider.notifier).refresh();
            ref.invalidate(aiPurposePreferencesControllerProvider);
          },
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.page),
            children: [
              Text('推荐', style: Theme.of(context).textTheme.titleMedium),
              const Card(
                child: ListTile(
                  leading: Icon(Icons.auto_awesome),
                  title: Text('Auto'),
                  subtitle: Text('自动为当前任务选择合适模型'),
                  trailing: Icon(Icons.check),
                ),
              ),
              _section(context, '系统模型', items.systemModels),
              _section(context, '我的模型', items.userModels),
              const SizedBox(height: AppSpacing.large),
              Text('按功能偏好', style: Theme.of(context).textTheme.titleMedium),
              preferences.when(
                loading: () => const Padding(
                  padding: EdgeInsets.all(AppSpacing.medium),
                  child: LinearProgressIndicator(),
                ),
                error: (_, __) => const Text('偏好读取失败'),
                data: (values) => Column(
                  children: [
                    for (final preference in values)
                      _PreferenceTile(
                        preference: preference,
                        models: items.selectableModels,
                        onChanged: (value) async {
                          await ref
                              .read(aiPurposePreferencesControllerProvider.notifier)
                              .save(value);
                          await widget.onChanged();
                          if (value.purpose == AiPurpose.tutor && context.mounted) {
                            showAppSuccessSnackBar(
                              context,
                              '辅导偏好已更新；新会话将使用新的选择，当前会话保持不变',
                            );
                          }
                        },
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _section(BuildContext context, String title, List<AiModelSummary> models) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: AppSpacing.large),
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        if (models.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: AppSpacing.small),
            child: Text('暂无模型'),
          ),
        for (final model in models)
          Card(
            child: ListTile(
              onTap: model.isSystem ? null : () => setState(() => _editingModel = model),
              leading: Icon(model.isSystem ? Icons.verified_outlined : Icons.smart_toy_outlined),
              title: Text(model.displayName),
              subtitle: Text([
                if (model.isSystem) '内置',
                if (!model.enabled) '已停用',
                if (model.maskedApiKey?.isNotEmpty == true) 'Key：${model.maskedApiKey}',
              ].join(' · ')),
              trailing: model.isSystem
                  ? const Chip(label: Text('内置'))
                  : IconButton(
                      tooltip: '删除模型',
                      onPressed: () => _delete(model),
                      icon: const Icon(Icons.delete_outline),
                    ),
            ),
          ),
      ],
    );
  }

  Future<void> _delete(AiModelSummary model) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除 AI 模型',
      message: '确定删除“${model.displayName}”吗？',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed) return;
    try {
      await ref.read(aiModelCatalogControllerProvider.notifier).delete(model);
      await widget.onChanged();
      if (mounted) showAppSuccessSnackBar(context, 'AI 模型已删除');
    } catch (error) {
      if (mounted) showAppErrorSnackBar(context, appFailureUserMessage(error));
    }
  }
}

class _PreferenceTile extends StatelessWidget {
  const _PreferenceTile({
    required this.preference,
    required this.models,
    required this.onChanged,
  });

  final AiPurposePreference preference;
  final List<AiModelSummary> models;
  final ValueChanged<AiPurposePreference> onChanged;

  @override
  Widget build(BuildContext context) => Card(
        child: ExpansionTile(
          title: Text(preference.purpose.label),
          subtitle: Text(preference.mode == AiSelectionMode.auto ? 'Auto' : '手动模型'),
          children: [
            ListTile(
              title: const Text('Auto'),
              trailing: preference.mode == AiSelectionMode.auto
                  ? const Icon(Icons.check)
                  : null,
              onTap: () => onChanged(AiPurposePreference(
                purpose: preference.purpose,
                allowUserModelsInAuto: preference.allowUserModelsInAuto,
                allowFallbackInManual: preference.allowFallbackInManual,
              )),
            ),
            for (final model in models)
              ListTile(
                title: Text(model.displayName),
                trailing: preference.mode == AiSelectionMode.manual &&
                        preference.selectedModelId == model.id
                    ? const Icon(Icons.check)
                    : null,
                onTap: () => onChanged(AiPurposePreference(
                  purpose: preference.purpose,
                  mode: AiSelectionMode.manual,
                  selectedModelId: model.id,
                  allowUserModelsInAuto: preference.allowUserModelsInAuto,
                  allowFallbackInManual: preference.allowFallbackInManual,
                )),
            ),
            SwitchListTile(
              title: const Text('允许我的模型参与 Auto'),
              value: preference.allowUserModelsInAuto,
              onChanged: (value) => onChanged(AiPurposePreference(
                purpose: preference.purpose,
                mode: preference.mode,
                selectedModelId: preference.selectedModelId,
                allowUserModelsInAuto: value,
                allowFallbackInManual: preference.allowFallbackInManual,
              )),
            ),
            SwitchListTile(
              title: const Text('手动模式允许降级'),
              value: preference.allowFallbackInManual,
              onChanged: (value) => onChanged(AiPurposePreference(
                purpose: preference.purpose,
                mode: preference.mode,
                selectedModelId: preference.selectedModelId,
                allowUserModelsInAuto: preference.allowUserModelsInAuto,
                allowFallbackInManual: value,
              )),
            ),
          ],
        ),
      );
}
