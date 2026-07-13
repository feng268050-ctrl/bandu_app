import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  final root = Directory.current;
  final lib = Directory('${root.path}/lib');
  final components = Directory('${lib.path}/components');

  test('components stay independent from state, features, and framework', () {
    const forbidden = [
      'package:flutter_riverpod/',
      'package:bandu_wrong_notebook/application/features/',
      'package:bandu_wrong_notebook/framework/',
      'ConsumerWidget',
      'ConsumerState',
      'Provider<',
      'Repository',
    ];

    for (final file in _dartFiles(components)) {
      final source = file.readAsStringSync();
      for (final token in forbidden) {
        expect(source, isNot(contains(token)), reason: '${file.path}: $token');
      }
    }
  });

  test('feature pages do not define common visual styles or fake buttons', () {
    const forbidden = [
      'ThemeData(',
      'Color(0x',
      'Colors.',
      'BorderRadius.circular(',
      'GestureDetector(',
      'Container(',
    ];
    final featureRoot = Directory('${lib.path}/application/features');
    final pageFiles = _dartFiles(
      featureRoot,
    ).where((file) => file.path.endsWith('_page.dart'));

    for (final file in pageFiles) {
      final source = file.readAsStringSync();
      for (final token in forbidden) {
        expect(source, isNot(contains(token)), reason: '${file.path}: $token');
      }
    }
  });

  test('required Material component building blocks are implemented', () {
    const paths = [
      'design_system/tokens/app_spacing.dart',
      'design_system/tokens/app_radius.dart',
      'design_system/tokens/app_sizes.dart',
      'design_system/tokens/app_duration.dart',
      'actions/app_primary_button.dart',
      'actions/app_secondary_button.dart',
      'actions/app_default_button.dart',
      'actions/app_async_primary_button.dart',
      'feedback/app_loading_view.dart',
      'feedback/app_error_view.dart',
      'feedback/app_empty_view.dart',
      'feedback/app_snackbars.dart',
      'forms/app_password_field.dart',
      'forms/app_form_section.dart',
      'dialogs/app_confirm_dialog.dart',
      'media/app_avatar.dart',
      'media/local_file_image.dart',
    ];

    for (final path in paths) {
      expect(File('${components.path}/$path').existsSync(), isTrue,
          reason: path);
    }
    expect(
      File('${components.path}/feedback/app_async_view.dart').existsSync(),
      isFalse,
    );
  });

  test('button levels use native Material 3 buttons', () {
    final primary = File(
      '${components.path}/actions/app_primary_button.dart',
    ).readAsStringSync();
    final secondary = File(
      '${components.path}/actions/app_secondary_button.dart',
    ).readAsStringSync();
    final defaultButton = File(
      '${components.path}/actions/app_default_button.dart',
    ).readAsStringSync();

    expect(primary, contains('FilledButton'));
    expect(secondary, contains('OutlinedButton'));
    expect(defaultButton, contains('OutlinedButton'));
    expect(primary, isNot(contains('GestureDetector')));
    expect(secondary, isNot(contains('GestureDetector')));
    expect(defaultButton, isNot(contains('GestureDetector')));
  });
}

Iterable<File> _dartFiles(Directory directory) {
  return directory
      .listSync(recursive: true)
      .whereType<File>()
      .where((file) => file.path.endsWith('.dart'));
}
