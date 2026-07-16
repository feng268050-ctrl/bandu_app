import 'package:bandu_wrong_notebook/application/app/app_failure.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_controller.dart';
import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_editor_page.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_empty_view.dart';
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
  static const _maxConfigs = 10;

  bool _isEditing = false;
  AiServiceConfig? _editingConfig;

  @override
  Widget build(BuildContext context) {
    if (_isEditing) {
      return AiConfigEditorPage(
        config: _editingConfig,
        onBack: _closeEditor,
        onSave: _save,
      );
    }

    final configs = ref.watch(aiConfigControllerProvider);
    final canAdd = (configs.valueOrNull?.length ?? _maxConfigs) < _maxConfigs;
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI 配置'),
        leading: BackButton(onPressed: widget.onBack),
        actions: [
          IconButton(
            tooltip: canAdd ? '新增配置' : '已达到配置上限',
            onPressed: canAdd ? _create : null,
            icon: const Icon(Icons.add),
          ),
        ],
      ),
      body: configs.when(
        loading: () => const AppLoadingView(message: '正在读取 AI 配置'),
        error: (error, _) => AppErrorView(
          message: appFailureUserMessage(error),
          onRetry: ref.read(aiConfigControllerProvider.notifier).refresh,
        ),
        data: (items) {
          if (items.isEmpty) {
            return AppEmptyView(
              title: '尚未配置 AI 服务',
              message: '当前没有可供 AI 辅导选择的模型',
              icon: Icons.smart_toy_outlined,
              actionLabel: '新增配置',
              onAction: _create,
            );
          }
          return RefreshIndicator(
            onRefresh: ref.read(aiConfigControllerProvider.notifier).refresh,
            child: ListView.separated(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(AppSpacing.page),
              itemCount: items.length,
              separatorBuilder: (_, __) =>
                  const SizedBox(height: AppSpacing.small),
              itemBuilder: (context, index) => _AiConfigCard(
                config: items[index],
                onEdit: () => _edit(items[index]),
                onSetDefault: items[index].isDefault
                    ? null
                    : () => _setDefault(items[index]),
                onDelete: () => _delete(items[index]),
              ),
            ),
          );
        },
      ),
    );
  }

  void _create() {
    setState(() {
      _editingConfig = null;
      _isEditing = true;
    });
  }

  void _edit(AiServiceConfig config) {
    setState(() {
      _editingConfig = config;
      _isEditing = true;
    });
  }

  void _closeEditor() {
    setState(() {
      _editingConfig = null;
      _isEditing = false;
    });
  }

  Future<void> _save(AiServiceConfigDraft draft) async {
    final editingId = _editingConfig?.id;
    try {
      await ref.read(aiConfigControllerProvider.notifier).save(
            id: editingId,
            draft: draft,
          );
      await widget.onChanged();
      if (!mounted) return;
      showAppSuccessSnackBar(
          context, editingId == null ? 'AI 配置已新增' : 'AI 配置已保存');
      _closeEditor();
    } catch (error) {
      if (mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
  }

  Future<void> _setDefault(AiServiceConfig config) async {
    try {
      await ref.read(aiConfigControllerProvider.notifier).setDefault(config.id);
      await widget.onChanged();
      if (mounted) {
        showAppSuccessSnackBar(context, '已切换默认模型');
      }
    } catch (error) {
      if (mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
  }

  Future<void> _delete(AiServiceConfig config) async {
    final confirmed = await showAppConfirmDialog(
      context: context,
      title: '删除 AI 配置',
      message: '确定删除“${config.name}”吗？',
      confirmLabel: '删除',
      isDestructive: true,
    );
    if (!confirmed) return;

    try {
      await ref.read(aiConfigControllerProvider.notifier).delete(config.id);
      await widget.onChanged();
      if (mounted) {
        showAppSuccessSnackBar(context, 'AI 配置已删除');
      }
    } catch (error) {
      if (mounted) {
        showAppErrorSnackBar(context, appFailureUserMessage(error));
      }
    }
  }
}

enum _AiConfigMenuAction { setDefault, delete }

class _AiConfigCard extends StatelessWidget {
  const _AiConfigCard({
    required this.config,
    required this.onEdit,
    required this.onSetDefault,
    required this.onDelete,
  });

  final AiServiceConfig config;
  final VoidCallback onEdit;
  final VoidCallback? onSetDefault;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: ListTile(
        onTap: onEdit,
        leading: Icon(
          config.isDefault ? Icons.check_circle : Icons.smart_toy_outlined,
        ),
        title: Row(
          children: [
            Expanded(
              child: Text(
                config.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            if (config.isDefault)
              const Tooltip(
                message: '默认模型',
                child: Icon(Icons.star, size: 18),
              ),
          ],
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: AppSpacing.small),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                config.model,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              Text(
                config.baseUrl,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              Text(
                config.hasApiKey
                    ? 'API Key：${config.maskedApiKey}'
                    : 'API Key：未保存',
              ),
            ],
          ),
        ),
        trailing: PopupMenuButton<_AiConfigMenuAction>(
          tooltip: '配置操作',
          icon: const Icon(Icons.more_vert),
          onSelected: (action) {
            switch (action) {
              case _AiConfigMenuAction.setDefault:
                onSetDefault?.call();
              case _AiConfigMenuAction.delete:
                onDelete();
            }
          },
          itemBuilder: (context) => [
            if (onSetDefault != null)
              const PopupMenuItem(
                value: _AiConfigMenuAction.setDefault,
                child: ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(Icons.check_circle_outline),
                  title: Text('设为默认'),
                ),
              ),
            const PopupMenuItem(
              value: _AiConfigMenuAction.delete,
              child: ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.delete_outline),
                title: Text('删除'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
