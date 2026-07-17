import 'package:bandu_wrong_notebook/application/features/ai_config/domain/ai_config_models.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';

class AiConfigEditorPage extends StatefulWidget {
  const AiConfigEditorPage({
    required this.onBack,
    required this.onSave,
    this.model = const AiModelSummary(
      id: '',
      displayName: '',
      provider: '',
      modelName: '',
      kind: AiModelKind.user,
      enabled: true,
      participatesInAuto: true,
      capabilities: AiModelCapabilities(),
    ),
    super.key,
  });

  final AiModelSummary model;
  final VoidCallback onBack;
  final Future<void> Function(AiModelDraft draft) onSave;

  @override
  State<AiConfigEditorPage> createState() => _AiConfigEditorPageState();
}

class _AiConfigEditorPageState extends State<AiConfigEditorPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name;
  late final TextEditingController _provider;
  late final TextEditingController _model;
  late final TextEditingController _baseUrl;
  final _apiKey = TextEditingController();
  late bool _enabled;
  late bool _auto;
  late bool _vision;
  late bool _text;
  late bool _json;
  bool _saving = false;
  bool get _editing => widget.model.id.isNotEmpty;

  @override
  void initState() {
    super.initState();
    final model = widget.model;
    _name = TextEditingController(text: model.displayName);
    _provider = TextEditingController(text: model.provider);
    _model = TextEditingController(text: model.modelName);
    _baseUrl = TextEditingController(text: model.baseUrl ?? '');
    _enabled = model.enabled;
    _auto = model.participatesInAuto;
    _vision = model.capabilities.supportsVision;
    _text = model.capabilities.supportsText;
    _json = model.capabilities.supportsJson;
  }

  @override
  void dispose() {
    _name.dispose();
    _provider.dispose();
    _model.dispose();
    _baseUrl.dispose();
    _apiKey.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: Text(_editing ? '编辑模型' : '添加模型'),
          leading: BackButton(onPressed: widget.onBack),
        ),
        body: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.page),
            children: [
              _field(_name, '配置名称'),
              _field(_provider, 'Provider'),
              _field(_model, 'Model Name'),
              _field(_baseUrl, 'Base URL', required: false, url: true),
              TextFormField(
                controller: _apiKey,
                obscureText: true,
                autocorrect: false,
                enableSuggestions: false,
                enableInteractiveSelection: false,
                decoration: InputDecoration(
                  labelText: _editing ? 'API Key（留空不修改）' : 'API Key',
                  helperText: _editing && widget.model.maskedApiKey != null
                      ? '已保存：${widget.model.maskedApiKey}'
                      : null,
                ),
                validator: (value) => !_editing && (value?.trim().isEmpty ?? true)
                    ? '新增模型必须填写 API Key'
                    : null,
              ),
              const SizedBox(height: AppSpacing.medium),
              Text('能力', style: Theme.of(context).textTheme.titleMedium),
              SwitchListTile(
                title: const Text('支持图片'),
                value: _vision,
                onChanged: _saving ? null : (value) => setState(() => _vision = value),
              ),
              SwitchListTile(
                title: const Text('支持文本'),
                value: _text,
                onChanged: _saving ? null : (value) => setState(() => _text = value),
              ),
              SwitchListTile(
                title: const Text('支持 JSON'),
                value: _json,
                onChanged: _saving ? null : (value) => setState(() => _json = value),
              ),
              SwitchListTile(
                title: const Text('启用模型'),
                value: _enabled,
                onChanged: _saving ? null : (value) => setState(() => _enabled = value),
              ),
              SwitchListTile(
                title: const Text('参与 Auto'),
                value: _auto,
                onChanged: _saving ? null : (value) => setState(() => _auto = value),
              ),
              const SizedBox(height: AppSpacing.large),
              AppAsyncPrimaryButton(
                label: '保存模型',
                expanded: true,
                isLoading: _saving,
                onPressed: _save,
              ),
            ],
          ),
        ),
      );

  Widget _field(
    TextEditingController controller,
    String label, {
    bool required = true,
    bool url = false,
  }) =>
      TextFormField(
        controller: controller,
        keyboardType: url ? TextInputType.url : null,
        decoration: InputDecoration(labelText: label),
        validator: (value) {
          final text = value?.trim() ?? '';
          if (required && text.isEmpty) return '此项不能为空';
          if (url && text.isNotEmpty) {
            final uri = Uri.tryParse(text);
            if (uri == null || !uri.hasAuthority) return '请输入有效地址';
          }
          return null;
        },
      );

  Future<void> _save() async {
    if (_saving || !_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      await widget.onSave(AiModelDraft(
        displayName: _name.text,
        provider: _provider.text,
        modelName: _model.text,
        baseUrl: _baseUrl.text.trim().isEmpty ? null : _baseUrl.text,
        apiKey: _apiKey.text,
        enabled: _enabled,
        participatesInAuto: _auto,
        capabilities: AiModelCapabilities(
          supportsVision: _vision,
          supportsText: _text,
          supportsJson: _json,
        ),
      ));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }
}
