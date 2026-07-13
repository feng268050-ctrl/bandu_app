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
}
