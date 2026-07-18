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

  void _openEditor([AiModelSummary? model]) {
    setState(() {
      _editingModel = model ??
          const AiModelSummary(
            id: '',
            displayName: '',
            provider: '',
            modelName: '',
            kind: AiModelKind.user,
            enabled: true,
            participatesInAuto: true,
            capabilities: AiModelCapabilities(),
          );
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_editingModel != null) {
      return AiConfigEditorPage(
        model: _editingModel!,
        onBack: () => setState(() => _editingModel = null),
        onSave: (draft) async {
          await ref.read(aiModelCatalogControllerProvider.notifier).save(
                draft,
                id: _editingModel?.id.isEmpty == true ? null : _editingModel?.id,
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
      ),
      body: catalog.when(
        skipLoadingOnReload: true,
        loading: () => const AppLoadingView(message: '正在读取 AI 模型'),
        error: (error, _) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: () => ref
              .read(aiModelCatalogControllerProvider.notifier)
              .refresh(forceLoading: true),
        ),
        data: (items) => RefreshIndicator(
          onRefresh: () async {
            await ref.read(aiModelCatalogControllerProvider.notifier).refresh();
            ref.invalidate(aiPurposePreferencesControllerProvider);
          },
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.page),
            children: [
              _ModelSwitchSection(
                title: '系统模型',
                models: items.configuredSystemModels,
                emptyLabel: '暂无已配置的系统模型',
                onToggle: (model, enabled) => _toggleEnabled(model, enabled),
              ),
              _ModelSwitchSection(
                title: '我的模型',
                models: items.configuredUserModels,
                emptyLabel: '暂无模型',
                emptyAction: _AddModelLink(onTap: () => _openEditor()),
                onToggle: (model, enabled) => _toggleEnabled(model, enabled),
                onEdit: (model) => _openEditor(model),
                onDelete: _delete,
              ),
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
                              .read(
                                aiPurposePreferencesControllerProvider.notifier,
                              )
                              .save(value);
                          await widget.onChanged();
                          if (value.purpose == AiPurpose.tutor &&
                              context.mounted) {
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

  Future<void> _toggleEnabled(AiModelSummary model, bool enabled) async {
    try {
      await ref
          .read(aiModelCatalogControllerProvider.notifier)
          .setEnabled(model, enabled);
      await widget.onChanged();
    } catch (error) {
      if (mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
  }

  Future<void> _delete(AiModelSummary model) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除 AI 模型',
      message: '确定删除“${model.listLabel}”吗？',
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

class _ModelSwitchSection extends StatelessWidget {
  const _ModelSwitchSection({
    required this.title,
    required this.models,
    required this.emptyLabel,
    required this.onToggle,
    this.emptyAction,
    this.onEdit,
    this.onDelete,
  });

  final String title;
  final List<AiModelSummary> models;
  final String emptyLabel;
  final Widget? emptyAction;
  final Future<void> Function(AiModelSummary model, bool enabled)? onToggle;
  final ValueChanged<AiModelSummary>? onEdit;
  final ValueChanged<AiModelSummary>? onDelete;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: AppSpacing.large),
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        if (models.isEmpty) ...[
          Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.small),
            child: Text(
              emptyLabel,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
            ),
          ),
          if (emptyAction != null) emptyAction!,
        ] else ...[
          Card(
            clipBehavior: Clip.antiAlias,
            child: Column(
              children: [
                for (var index = 0; index < models.length; index++) ...[
                  if (index > 0) const Divider(height: 1),
                  _ModelSwitchTile(
                    model: models[index],
                    onToggle: onToggle,
                    onEdit: onEdit,
                    onDelete: onDelete,
                  ),
                ],
              ],
            ),
          ),
          if (emptyAction != null) ...[
            const SizedBox(height: AppSpacing.small),
            emptyAction!,
          ],
        ],
      ],
    );
  }
}

class _AddModelLink extends StatelessWidget {
  const _AddModelLink({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final style = Theme.of(context).textTheme.bodySmall?.copyWith(
          color: colorScheme.primary,
          decoration: TextDecoration.underline,
          decorationColor: colorScheme.primary,
          fontSize: 12,
        );
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.xSmall),
      child: Align(
        alignment: Alignment.centerLeft,
        child: InkWell(
          onTap: onTap,
          child: Text('新增模型', style: style),
        ),
      ),
    );
  }
}

class _ModelSwitchTile extends StatelessWidget {
  const _ModelSwitchTile({
    required this.model,
    required this.onToggle,
    this.onEdit,
    this.onDelete,
  });

  final AiModelSummary model;
  final Future<void> Function(AiModelSummary model, bool enabled)? onToggle;
  final ValueChanged<AiModelSummary>? onEdit;
  final ValueChanged<AiModelSummary>? onDelete;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: model.isSystem || onEdit == null ? null : () => onEdit!(model),
      title: Text(model.listLabel),
      subtitle: model.isSystem
          ? const Text('系统配置')
          : (model.maskedApiKey?.isNotEmpty == true
              ? Text('Key：${model.maskedApiKey}')
              : null),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (!model.isSystem && onDelete != null)
            IconButton(
              tooltip: '删除模型',
              onPressed: () => onDelete!(model),
              icon: const Icon(Icons.delete_outline),
            ),
          Switch.adaptive(
            value: model.enabled,
            onChanged: onToggle == null
                ? null
                : (value) => onToggle!(model, value),
          ),
        ],
      ),
    );
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
          subtitle: Text(
            preference.mode == AiSelectionMode.auto ? 'Auto' : '手动模型',
          ),
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
                title: Text(model.listLabel),
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
