import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

final captureFileStoreProvider = Provider<CaptureFileStore>((ref) {
  return CaptureFileStore();
});

class CapturedFile {
  const CapturedFile({required this.id, required this.path});

  final String id;
  final String path;
}

class CaptureFileStore {
  Future<CapturedFile> persistOriginal(File source) async {
    final root = await getApplicationDocumentsDirectory();
    final captureId = DateTime.now().microsecondsSinceEpoch.toString();
    final directory = Directory(
      p.join(root.path, 'bandu', 'capture', captureId),
    );

    await directory.create(recursive: true);
    final target = File(
      p.join(directory.path, 'original${p.extension(source.path)}'),
    );
    await source.copy(target.path);

    return CapturedFile(id: captureId, path: target.path);
  }

  Future<bool> captureExists(String filePath) async {
    final root = await _captureRoot();
    final absoluteRoot = p.normalize(root.absolute.path);
    final absoluteFile = p.normalize(File(filePath).absolute.path);
    if (!p.isWithin(absoluteRoot, absoluteFile)) {
      return false;
    }
    return File(absoluteFile).exists();
  }

  Future<void> deleteCapture(String filePath) async {
    final root = await _captureRoot();
    final absoluteRoot = p.normalize(root.absolute.path);
    final absoluteFile = p.normalize(File(filePath).absolute.path);
    if (!p.isWithin(absoluteRoot, absoluteFile)) {
      return;
    }

    final relative = p.relative(absoluteFile, from: absoluteRoot);
    final segments = p.split(relative);
    if (segments.isEmpty || segments.first == '..') {
      return;
    }
    final captureDirectory = Directory(p.join(absoluteRoot, segments.first));
    if (await captureDirectory.exists()) {
      await captureDirectory.delete(recursive: true);
    }
  }

  Future<void> clearAllCaptures() async {
    final root = await _captureRoot();
    if (await root.exists()) {
      await root.delete(recursive: true);
    }
  }

  Future<Directory> _captureRoot() async {
    final root = await getApplicationDocumentsDirectory();
    return Directory(p.join(root.path, 'bandu', 'capture'));
  }
}
