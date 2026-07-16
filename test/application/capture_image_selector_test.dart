import 'package:bandu_wrong_notebook/application/features/capture/presentation/widgets/capture_image_selector.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('capture source actions stay single-line on a narrow phone', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildAppTheme(Brightness.dark),
        home: Scaffold(
          body: Center(
            child: SizedBox(
              width: 315,
              child: CaptureImageSelector(
                imagePath: null,
                isBusy: false,
                onTakePhoto: () {},
                onPickFromGallery: () {},
                onImportPdf: () {},
              ),
            ),
          ),
        ),
      ),
    );

    final pdfButton = tester.getRect(find.byType(FilledButton));
    final galleryButton = tester.getRect(find.byType(OutlinedButton));

    expect(pdfButton.width, greaterThan(galleryButton.width));
    expect(pdfButton.height, galleryButton.height);
    expect(tester.getSize(find.text('导入 PDF 题集')).height, lessThan(24));
  });
}
