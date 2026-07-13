import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/forms/app_form_section.dart';
import 'package:flutter/material.dart';

class AiConfigEditorPage extends StatefulWidget {
  const AiConfigEditorPage({
    required this.onBack,
    required this.onSave,
    this.config,
    super.key,
  });

  final AiServiceConfig? config;
  final VoidCallback onBack;
  final Future<void> Function(AiServiceConfigDraft draft) onSave;

  @override
  State<AiConfigEditorPage> createState() => _AiConfigEditorPageState();
}

class _AiConfigEditorPageState extends State<AiConfigEditorPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _baseUrlController;
  late final TextEditingController _apiKeyController;
  late final TextEditingController _modelController;
  late bool _isDefault;
  bool _isSaving = false;

  bool get _isEditing => widget.config != null;

  @override
  void initState() {
    super.initState();
    final config = widget.config;
    _nameController = TextEditingController(text: config?.name ?? '');
    _baseUrlController = TextEditingController(
      text: config?.baseUrl ?? 'https://api.openai.com/v1',
    );
    _apiKeyController = TextEditingController();
    _modelController = TextEditingController(text: config?.model ?? '');
    _isDefault = config?.isDefault ?? false;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _baseUrlController.dispose();
    _apiKeyController.dispose();
    _modelController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? '编辑 AI 配置' : '新增 AI 配置'),
        leading: BackButton(onPressed: widget.onBack),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.page),
          children: [
            AppFormSection(
              title: '服务信息',
              children: [
                TextFormField(
                  controller: _nameController,
                  textInputAction: TextInputAction.next,
                  maxLength: 60,
                  decoration: const InputDecoration(
                    labelText: '配置名称',
                    prefixIcon: Icon(Icons.label_outline),
                  ),
                  validator: _requiredValidator,
                ),
                TextFormField(
                  controller: _baseUrlController,
                  keyboardType: TextInputType.url,
                  textInputAction: TextInputAction.next,
                  autocorrect: false,
                  decoration: const InputDecoration(
                    labelText: 'API 地址',
                    prefixIcon: Icon(Icons.link),
                  ),
                  validator: _urlValidator,
                ),
                TextFormField(
                  controller: _apiKeyController,
                  obscureText: true,
                  enableInteractiveSelection: false,
                  autocorrect: false,
                  enableSuggestions: false,
                  keyboardType: TextInputType.visiblePassword,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(
                    labelText: _isEditing ? 'API Key（留空保留）' : 'API Key',
                    prefixIcon: const Icon(Icons.key_outlined),
                  ),
                  validator: (value) {
                    if (_isEditing && (value == null || value.trim().isEmpty)) {
                      return null;
                    }
                    return _requiredValidator(value);
                  },
                ),
                TextFormField(
                  controller: _modelController,
                  textInputAction: TextInputAction.done,
                  maxLength: 200,
                  decoration: const InputDecoration(
                    labelText: '模型名称',
                    prefixIcon: Icon(Icons.smart_toy_outlined),
                  ),
                  validator: _requiredValidator,
                  onFieldSubmitted: (_) => _save(),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('设为默认模型'),
                  secondary: const Icon(Icons.check_circle_outline),
                  value: _isDefault,
                  onChanged: _isSaving
                      ? null
                      : (value) => setState(() => _isDefault = value),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.xLarge),
            AppAsyncPrimaryButton(
              label: '保存配置',
              expanded: true,
              isLoading: _isSaving,
              icon: const Icon(Icons.save_outlined),
              onPressed: _save,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _save() async {
    if (_isSaving || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _isSaving = true);
    try {
      await widget.onSave(
        AiServiceConfigDraft(
          name: _nameController.text.trim(),
          baseUrl: _baseUrlController.text.trim(),
          apiKey: _apiKeyController.text.trim(),
          model: _modelController.text.trim(),
          isDefault: _isDefault,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }

  String? _requiredValidator(String? value) {
    return value == null || value.trim().isEmpty ? '此项不能为空' : null;
  }

  String? _urlValidator(String? value) {
    final text = value?.trim() ?? '';
    final uri = Uri.tryParse(text);
    if (uri == null ||
        !uri.hasAuthority ||
        (uri.scheme != 'http' && uri.scheme != 'https')) {
      return '请输入有效的 HTTP 或 HTTPS 地址';
    }
    return null;
  }
}
