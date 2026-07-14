import 'package:bandu_wrong_notebook/application/features/auth/domain/auth_models.dart';
import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/feedback/app_snackbars.dart';
import 'package:bandu_wrong_notebook/components/forms/app_form_section.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class StudentProfileEditorPage extends ConsumerStatefulWidget {
  const StudentProfileEditorPage({
    required this.user,
    required this.onBack,
    super.key,
  });

  final UserProfile? user;
  final VoidCallback onBack;

  @override
  ConsumerState<StudentProfileEditorPage> createState() =>
      _StudentProfileEditorPageState();
}

class _StudentProfileEditorPageState
    extends ConsumerState<StudentProfileEditorPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _yearController;
  late String _educationStage;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.user?.name);
    _yearController = TextEditingController(
      text: widget.user?.enrollmentYear?.toString() ?? '',
    );
    _educationStage = widget.user?.educationStage ?? 'primary';
  }

  @override
  void dispose() {
    _nameController.dispose();
    _yearController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('学生资料'),
        leading: BackButton(onPressed: widget.onBack),
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.page),
            children: [
              AppFormSection(
                title: '基本信息',
                description: '年级将根据当前年份和入学年份自动计算。',
                children: [
                  TextFormField(
                    controller: _nameController,
                    enabled: !authState.isBusy,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      labelText: '昵称',
                      prefixIcon: Icon(Icons.person_outline),
                    ),
                    validator: (value) =>
                        (value ?? '').trim().isEmpty ? '昵称不能为空' : null,
                  ),
                  DropdownMenu<String>(
                    initialSelection: _educationStage,
                    enabled: !authState.isBusy,
                    expandedInsets: EdgeInsets.zero,
                    label: const Text('教育阶段'),
                    leadingIcon: const Icon(Icons.school_outlined),
                    dropdownMenuEntries: const [
                      DropdownMenuEntry(value: 'primary', label: '小学'),
                      DropdownMenuEntry(value: 'junior_high', label: '初中'),
                      DropdownMenuEntry(value: 'senior_high', label: '高中'),
                      DropdownMenuEntry(value: 'university', label: '大学'),
                    ],
                    onSelected: (value) {
                      if (value != null) {
                        setState(() => _educationStage = value);
                      }
                    },
                  ),
                  TextFormField(
                    controller: _yearController,
                    enabled: !authState.isBusy,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.done,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(
                      labelText: '入学年份',
                      hintText: '例如 2022',
                      prefixIcon: Icon(Icons.calendar_today_outlined),
                    ),
                    validator: (value) {
                      final year = int.tryParse(value?.trim() ?? '');
                      final currentYear = DateTime.now().year;
                      if (year == null || year < 1900 || year > currentYear) {
                        return '请输入有效的入学年份';
                      }
                      return _validateGradeYear(
                        educationStage: _educationStage,
                        enrollmentYear: year,
                      );
                    },
                    onFieldSubmitted: (_) {
                      if (!authState.isBusy) {
                        _save();
                      }
                    },
                  ),
                  if (_errorMessage != null)
                    Text(
                      _errorMessage!,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: colorScheme.error,
                          ),
                    ),
                  AppAsyncPrimaryButton(
                    label: '保存',
                    expanded: true,
                    isLoading: authState.isBusy,
                    icon: const Icon(Icons.save_outlined),
                    onPressed: _save,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _save() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (_formKey.currentState?.validate() != true) {
      return;
    }

    final saved = await ref.read(authControllerProvider.notifier).updateProfile(
          name: _nameController.text.trim(),
          educationStage: _educationStage,
          enrollmentYear: int.parse(_yearController.text.trim()),
        );
    if (!mounted) {
      return;
    }
    if (saved) {
      showAppSuccessSnackBar(context, '学生资料已保存');
      widget.onBack();
    } else {
      setState(() {
        _errorMessage = ref.read(authControllerProvider).errorMessage ?? '保存失败';
      });
    }
  }
}

String? _validateGradeYear({
  required String educationStage,
  required int enrollmentYear,
}) {
  final grade = DateTime.now().year - enrollmentYear;
  if (grade < 1) {
    return '入学年份必须早于当前年份';
  }

  final maxGrade = _maxGradeForStage(educationStage);
  if (maxGrade != null && grade > maxGrade) {
    return '${_educationStageLabel(educationStage)}年级不能超过 $maxGrade 年级';
  }

  return null;
}

int? _maxGradeForStage(String educationStage) {
  return switch (educationStage) {
    'primary' => 6,
    'junior_high' || 'senior_high' => 3,
    'university' => 4,
    _ => null,
  };
}

String _educationStageLabel(String educationStage) {
  return switch (educationStage) {
    'primary' => '小学',
    'junior_high' => '初中',
    'senior_high' => '高中',
    'university' => '大学',
    _ => '当前教育阶段',
  };
}
