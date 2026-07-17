import 'package:bandu_wrong_notebook/application/features/ai_config/presentation/ai_config_editor_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('API key input stays obscured and cannot be selected',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: AiConfigEditorPage(
          onBack: () {},
          onSave: (_) async {},
        ),
      ),
    );

    final apiKeyField = tester.widget<EditableText>(
      find.byType(EditableText).at(4),
    );

    expect(apiKeyField.obscureText, isTrue);
    expect(apiKeyField.enableInteractiveSelection, isFalse);
    expect(find.byIcon(Icons.visibility_outlined), findsNothing);
    expect(find.byIcon(Icons.visibility_off_outlined), findsNothing);
  });
}
