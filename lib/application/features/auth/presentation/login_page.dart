import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:bandu_wrong_notebook/components/forms/app_password_field.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _nameController = TextEditingController();
  bool _isRegisterMode = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('账号')),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Form(
              key: _formKey,
              child: ListView(
                shrinkWrap: true,
                padding: const EdgeInsets.all(AppSpacing.xLarge),
                children: [
                  Text(
                    '伴读题集',
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  const SizedBox(height: AppSpacing.small),
                  Text(
                    _isRegisterMode ? '创建移动端账号' : '登录移动端账号',
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                  ),
                  const SizedBox(height: AppSpacing.xLarge),
                  SegmentedButton<bool>(
                    segments: const [
                      ButtonSegment(
                        value: false,
                        icon: Icon(Icons.login),
                        label: Text('登录'),
                      ),
                      ButtonSegment(
                        value: true,
                        icon: Icon(Icons.person_add_outlined),
                        label: Text('注册'),
                      ),
                    ],
                    selected: {_isRegisterMode},
                    onSelectionChanged: authState.isBusy
                        ? null
                        : (selection) {
                            setState(() => _isRegisterMode = selection.single);
                          },
                  ),
                  const SizedBox(height: AppSpacing.xLarge),
                  if (_isRegisterMode) ...[
                    TextFormField(
                      controller: _nameController,
                      enabled: !authState.isBusy,
                      textInputAction: TextInputAction.next,
                      autofillHints: const [AutofillHints.name],
                      decoration: const InputDecoration(
                        labelText: '昵称',
                        prefixIcon: Icon(Icons.person_outline),
                      ),
                      validator: (value) {
                        if (_isRegisterMode && (value ?? '').trim().isEmpty) {
                          return '请输入昵称';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppSpacing.medium),
                  ],
                  TextFormField(
                    controller: _emailController,
                    enabled: !authState.isBusy,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.email],
                    decoration: const InputDecoration(
                      labelText: '邮箱',
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    validator: (value) {
                      final email = value?.trim() ?? '';
                      if (email.isEmpty) {
                        return '请输入邮箱';
                      }
                      if (!email.contains('@')) {
                        return '请输入有效邮箱';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: AppSpacing.medium),
                  AppPasswordField(
                    controller: _passwordController,
                    enabled: !authState.isBusy,
                    validator: (value) {
                      if ((value ?? '').isEmpty) {
                        return '请输入密码';
                      }
                      if ((value ?? '').length < 6) {
                        return '密码至少 6 位';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) {
                      if (!authState.isBusy) {
                        _submit();
                      }
                    },
                  ),
                  const SizedBox(height: AppSpacing.large),
                  AppAsyncPrimaryButton(
                    label: _isRegisterMode ? '注册并登录' : '登录',
                    isLoading: authState.isBusy,
                    expanded: true,
                    icon: Icon(
                      _isRegisterMode ? Icons.person_add_outlined : Icons.login,
                    ),
                    onPressed: _submit,
                  ),
                  if (authState.errorMessage != null) ...[
                    const SizedBox(height: AppSpacing.medium),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.error_outline, color: colorScheme.error),
                        const SizedBox(width: AppSpacing.small),
                        Expanded(
                          child: Text(
                            authState.errorMessage!,
                            style: Theme.of(context)
                                .textTheme
                                .bodyMedium
                                ?.copyWith(color: colorScheme.error),
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _submit() {
    FocusManager.instance.primaryFocus?.unfocus();
    if (_formKey.currentState?.validate() != true) {
      return;
    }

    final controller = ref.read(authControllerProvider.notifier);
    if (_isRegisterMode) {
      controller.register(
        email: _emailController.text.trim(),
        password: _passwordController.text,
        name: _nameController.text.trim(),
      );
    } else {
      controller.login(
        email: _emailController.text.trim(),
        password: _passwordController.text,
      );
    }
  }
}
