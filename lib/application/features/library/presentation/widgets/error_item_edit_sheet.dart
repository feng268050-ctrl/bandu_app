import 'package:bandu_wrong_notebook/application/features/library/domain/error_item.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/forms/app_form_section.dart';
import 'package:flutter/material.dart';

Future<bool> showErrorItemEditSheet({
  required BuildContext context,
  required ErrorItemDetail item,
  required Future<void> Function(ErrorItemUpdate update) onSave,
}) async {
  return await showModalBottomSheet<bool>(
        context: context,
        isScrollControlled: true,
        builder: (context) => _ErrorItemEditSheet(item: item, onSave: onSave),
      ) ??
      false;
}

class _ErrorItemEditSheet extends StatefulWidget {
  const _ErrorItemEditSheet({required this.item, required this.onSave});

  final ErrorItemDetail item;
  final Future<void> Function(ErrorItemUpdate update) onSave;

  @override
  State<_ErrorItemEditSheet> createState() => _ErrorItemEditSheetState();
}

class _ErrorItemEditSheetState extends State<_ErrorItemEditSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _questionController;
  late final TextEditingController _answerController;
  late final TextEditingController _analysisController;
  late int _masteryLevel;
  bool _saving = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _questionController = TextEditingController(text: widget.item.questionText);
    _answerController = TextEditingController(text: widget.item.answer);
    _analysisController = TextEditingController(text: widget.item.analysis);
    _masteryLevel = widget.item.masteryLevel;
  }

  @override
  void dispose() {
    _questionController.dispose();
    _answerController.dispose();
    _analysisController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          AppSpacing.page,
          0,
          AppSpacing.page,
          MediaQuery.viewInsetsOf(context).bottom + AppSpacing.page,
        ),
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: AppFormSection(
              title: '编辑错题',
              description: '修改题目内容和掌握状态。',
              children: [
                TextFormField(
                  controller: _questionController,
                  enabled: !_saving,
                  minLines: 3,
                  maxLines: 8,
                  decoration: const InputDecoration(labelText: '题目'),
                  validator: (value) =>
                      (value ?? '').trim().isEmpty ? '题目不能为空' : null,
                ),
                TextFormField(
                  controller: _answerController,
                  enabled: !_saving,
                  minLines: 2,
                  maxLines: 6,
                  decoration: const InputDecoration(labelText: '答案'),
                ),
                TextFormField(
                  controller: _analysisController,
                  enabled: !_saving,
                  minLines: 3,
                  maxLines: 8,
                  decoration: const InputDecoration(labelText: '解析'),
                ),
                DropdownMenu<int>(
                  initialSelection: _masteryLevel,
                  enabled: !_saving,
                  expandedInsets: EdgeInsets.zero,
                  label: const Text('掌握状态'),
                  dropdownMenuEntries: const [
                    DropdownMenuEntry(value: 0, label: '未掌握'),
                    DropdownMenuEntry(value: 1, label: '复习中'),
                    DropdownMenuEntry(value: 2, label: '已掌握'),
                  ],
                  onSelected: (value) {
                    if (value != null) {
                      setState(() => _masteryLevel = value);
                    }
                  },
                ),
                if (_errorMessage != null)
                  Text(
                    _errorMessage!,
                    style: Theme.of(
                      context,
                    ).textTheme.bodyMedium?.copyWith(color: colorScheme.error),
                  ),
                AppAsyncPrimaryButton(
                  label: '保存修改',
                  expanded: true,
                  isLoading: _saving,
                  icon: const Icon(Icons.save_outlined),
                  onPressed: _submit,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _submit() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (_formKey.currentState?.validate() != true) {
      return;
    }

    setState(() {
      _saving = true;
      _errorMessage = null;
    });
    try {
      await widget.onSave(
        ErrorItemUpdate(
          questionText: _questionController.text,
          answer: _answerController.text,
          analysis: _analysisController.text,
          masteryLevel: _masteryLevel,
        ),
      );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _saving = false;
          _errorMessage = error.toString();
        });
      }
    }
  }
}
