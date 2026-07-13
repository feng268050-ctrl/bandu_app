import 'dart:async';

import 'package:bandu_wrong_notebook/application/features/tutor/domain/tutor_repositories.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:speech_to_text/speech_to_text.dart';
import 'package:speech_to_text/speech_recognition_result.dart';

final platformSpeechInputRepositoryProvider =
    Provider<SpeechInputRepository>((ref) {
  return PlatformSpeechInputRepository(SpeechToText());
});

class PlatformSpeechInputRepository implements SpeechInputRepository {
  PlatformSpeechInputRepository(this._speech);

  final SpeechToText _speech;
  StreamController<String>? _controller;
  bool _initialized = false;

  @override
  Future<bool> initialize() async {
    if (_initialized) return _speech.isAvailable;
    _initialized = await _speech.initialize();
    return _initialized;
  }

  @override
  Stream<String> startListening() {
    _controller?.close();
    final controller = StreamController<String>.broadcast();
    _controller = controller;
    _speech.listen(
      onResult: (SpeechRecognitionResult result) {
        if (!controller.isClosed) {
          controller.add(result.recognizedWords);
        }
      },
      listenOptions: SpeechListenOptions(
        localeId: 'zh_CN',
        listenMode: ListenMode.dictation,
        partialResults: true,
      ),
    );
    return controller.stream;
  }

  @override
  Future<void> stopListening() async {
    await _speech.stop();
    await _controller?.close();
    _controller = null;
  }
}
