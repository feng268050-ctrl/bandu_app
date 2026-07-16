import 'package:bandu_wrong_notebook/application/app/app_shell.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('main navigation reserves the middle slot for the capture action',
      (tester) async {
    var selectedIndex = -1;

    await tester.pumpWidget(
      MaterialApp(
        theme: buildAppTheme(Brightness.light),
        home: Scaffold(
          floatingActionButtonLocation: appCaptureNavigationButtonLocation,
          floatingActionButton: AppCaptureNavigationButton(
            onPressed: () => selectedIndex = 2,
          ),
          bottomNavigationBar: AppBottomNavigationBar(
            selectedIndex: 0,
            onDestinationSelected: (index) => selectedIndex = index,
          ),
        ),
      ),
    );

    expect(find.text('首页'), findsOneWidget);
    expect(find.text('AI 辅导'), findsOneWidget);
    expect(find.text('练习'), findsOneWidget);
    expect(find.text('我的'), findsOneWidget);
    expect(find.text('拍题'), findsNothing);
    expect(find.byTooltip('拍题'), findsOneWidget);
    expect(find.byIcon(Icons.add_rounded), findsOneWidget);

    final bottomAppBar = tester.widget<BottomAppBar>(find.byType(BottomAppBar));
    expect(bottomAppBar.shape, isNull);
    expect(
      find.descendant(
        of: find.byType(BottomAppBar),
        matching: find.byType(Divider),
      ),
      findsOneWidget,
    );

    final captureButtonBottom =
        tester.getBottomRight(find.byType(FloatingActionButton)).dy;
    final navigationLabelCenter = tester.getCenter(find.text('首页')).dy;
    expect(captureButtonBottom, closeTo(navigationLabelCenter, 0.01));

    await tester.tap(find.text('AI 辅导'));
    expect(selectedIndex, 1);

    await tester.tap(find.byTooltip('拍题'));
    expect(selectedIndex, 2);
  });

  testWidgets('selected destination uses its filled icon', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildAppTheme(Brightness.light),
        home: Scaffold(
          bottomNavigationBar: AppBottomNavigationBar(
            selectedIndex: 3,
            onDestinationSelected: (_) {},
          ),
        ),
      ),
    );

    expect(find.byIcon(Icons.quiz), findsOneWidget);
    expect(find.byIcon(Icons.quiz_outlined), findsNothing);
    expect(find.byIcon(Icons.home_outlined), findsOneWidget);
  });
}
