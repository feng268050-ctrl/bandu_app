import 'package:bandu_wrong_notebook/components/actions/app_async_primary_button.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:bandu_wrong_notebook/components/dialogs/app_confirm_dialog.dart';
import 'package:bandu_wrong_notebook/components/forms/app_password_field.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('app theme configures the required Material 3 component families', () {
    final theme = buildAppTheme(Brightness.light);

    expect(theme.useMaterial3, isTrue);
    expect(theme.navigationBarTheme.height, 72);
    expect(theme.cardTheme.elevation, 0);
    expect(theme.inputDecorationTheme.border, isNotNull);
    expect(theme.filledButtonTheme.style, isNotNull);
    expect(theme.outlinedButtonTheme.style, isNotNull);
    expect(theme.textButtonTheme.style, isNotNull);
    expect(theme.iconButtonTheme.style, isNotNull);
    expect(theme.dialogTheme.shape, isNotNull);
    expect(theme.bottomSheetTheme.shape, isNotNull);
    expect(theme.snackBarTheme.behavior, SnackBarBehavior.floating);
    expect(theme.dividerTheme.thickness, 1);
  });

  testWidgets('async primary button disables duplicate taps while loading', (
    tester,
  ) async {
    var taps = 0;

    await tester.pumpWidget(
      _TestApp(
        child: AppAsyncPrimaryButton(
          label: '保存',
          isLoading: true,
          onPressed: () => taps += 1,
        ),
      ),
    );

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    await tester.tap(find.text('保存'));
    expect(taps, 0);

    await tester.pumpWidget(
      _TestApp(
        child: AppAsyncPrimaryButton(
          label: '保存',
          isLoading: false,
          onPressed: () => taps += 1,
        ),
      ),
    );
    await tester.tap(find.text('保存'));
    expect(taps, 1);
  });

  testWidgets('password field toggles visibility with an accessible icon', (
    tester,
  ) async {
    final controller = TextEditingController(text: 'secret');
    addTearDown(controller.dispose);

    await tester.pumpWidget(
      _TestApp(child: AppPasswordField(controller: controller)),
    );

    expect(
      tester.widget<EditableText>(find.byType(EditableText)).obscureText,
      isTrue,
    );
    await tester.tap(find.byTooltip('显示密码'));
    await tester.pump();
    expect(
      tester.widget<EditableText>(find.byType(EditableText)).obscureText,
      isFalse,
    );
    expect(find.byTooltip('隐藏密码'), findsOneWidget);
  });

  testWidgets('destructive confirm dialog returns true only after confirmation',
      (
    tester,
  ) async {
    bool? result;
    await tester.pumpWidget(
      MaterialApp(
        theme: buildAppTheme(Brightness.light),
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () async {
                result = await showAppConfirmDialog(
                  context: context,
                  title: '删除错题',
                  message: '删除后无法恢复。',
                  confirmLabel: '删除',
                  isDestructive: true,
                );
              },
              child: const Text('打开'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('打开'));
    await tester.pumpAndSettle();
    expect(find.byType(AppConfirmDialog), findsOneWidget);
    expect(find.text('取消'), findsOneWidget);
    expect(find.text('删除'), findsOneWidget);

    await tester.tap(find.text('删除'));
    await tester.pumpAndSettle();
    expect(result, isTrue);
  });
}

class _TestApp extends StatelessWidget {
  const _TestApp({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: buildAppTheme(Brightness.light),
      home: Scaffold(body: Center(child: child)),
    );
  }
}
