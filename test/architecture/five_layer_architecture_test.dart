import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  final root = Directory.current;
  final lib = Directory('${root.path}/lib');

  test('lib contains only the five-layer source roots', () {
    final entries =
        lib.listSync().map((entry) => _basename(entry.path)).toSet();

    expect(
      entries,
      equals({
        'application',
        'components',
        'conversion',
        'framework',
        'main.dart'
      }),
    );
    expect(Directory('${lib.path}/app').existsSync(), isFalse);
    expect(Directory('${lib.path}/core').existsSync(), isFalse);
    expect(Directory('${lib.path}/features').existsSync(), isFalse);
  });

  test('application does not reach framework or platform plugins', () {
    final files = _dartFiles(Directory('${lib.path}/application'));
    const forbidden = [
      'package:dio/',
      'package:camera/',
      'package:image_picker/',
      'package:flutter_secure_storage/',
      'MethodChannel(',
      "import 'dart:io'",
    ];

    for (final file in files) {
      final source = file.readAsStringSync();
      if (!file.path.endsWith('/application/app/bootstrap.dart')) {
        expect(
          source,
          isNot(contains('package:bandu_wrong_notebook/framework/')),
          reason: file.path,
        );
      }
      for (final token in forbidden) {
        expect(source, isNot(contains(token)), reason: '${file.path}: $token');
      }
    }
  });

  test('components remain independent from features and framework', () {
    for (final file in _dartFiles(Directory('${lib.path}/components'))) {
      final source = file.readAsStringSync();
      expect(
        source,
        isNot(contains('package:bandu_wrong_notebook/application/features/')),
        reason: file.path,
      );
      expect(
        source,
        isNot(contains('package:bandu_wrong_notebook/framework/')),
        reason: file.path,
      );
    }
  });

  test('conversion remains a pure boundary layer', () {
    const forbidden = [
      'package:bandu_wrong_notebook/framework/',
      'package:bandu_wrong_notebook/components/',
      'package:flutter_riverpod/',
      'package:flutter/material.dart',
    ];

    for (final file in _dartFiles(Directory('${lib.path}/conversion'))) {
      final source = file.readAsStringSync();
      for (final token in forbidden) {
        expect(source, isNot(contains(token)), reason: '${file.path}: $token');
      }
    }
  });

  test('json model factories live only in conversion', () {
    final files = _dartFiles(lib).where(
      (file) => !file.path.contains('/conversion/'),
    );
    final factoryPattern = RegExp(r'factory\s+\w+\.fromJson\s*\(');
    final encoderPattern = RegExp(
      r'Map<String,\s*Object\?>\s+toJson\s*\(',
    );

    for (final file in files) {
      final source = file.readAsStringSync();
      expect(factoryPattern.hasMatch(source), isFalse, reason: file.path);
      expect(encoderPattern.hasMatch(source), isFalse, reason: file.path);
    }
  });

  test('main delegates to the single composition bootstrap', () {
    final source = File('${lib.path}/main.dart').readAsStringSync();
    expect(source, contains('/application/app/bootstrap.dart'));
    expect(
        RegExp(r'^import ', multiLine: true).allMatches(source), hasLength(1));
  });
}

Iterable<File> _dartFiles(Directory directory) {
  return directory
      .listSync(recursive: true)
      .whereType<File>()
      .where((file) => file.path.endsWith('.dart'));
}

String _basename(String path) => path.split(Platform.pathSeparator).last;
