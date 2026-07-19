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
  String? _lastError;

  @override
  Future<bool> initialize() async {
    if (_initialized) return _speech.isAvailable;
    _lastError = null;
    _initialized = await _speech.initialize(
      onError: (error) {
        _lastError = error.errorMsg;
        final controller = _controller;
        if (controller != null && !controller.isClosed) {
          controller.addError(
            StateError(
              _speechErrorMessage(error.errorMsg) ?? '语音识别失败，请稍后重试',
            ),
          );
        }
      },
    );
    return _initialized && _speech.isAvailable;
  }

  @override
  Stream<String> startListening() {
    if (!_speech.isAvailable) {
      throw StateError(
        _speechErrorMessage(_lastError) ?? '当前设备无法使用语音输入',
      );
    }
    _controller?.close();
    final controller = StreamController<String>.broadcast();
    _controller = controller;
    unawaited(
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
          cancelOnError: true,
        ),
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

  String? _speechErrorMessage(String? code) {
    return switch (code) {
      'error_no_match' => '没有识别到有效语音，请靠近麦克风再说一次',
      'error_speech_timeout' || 'error_network_timeout' => '语音识别超时，请重试',
      'error_network' => '语音识别需要网络，请检查网络后重试',
      'error_audio' || 'error_microphone_busy' => '无法使用麦克风，请检查录音权限',
      'error_permission' || 'error_insufficient_permissions' =>
        '未授予麦克风权限，请在系统设置中开启',
      'error_language_not_supported' || 'error_language_unavailable' =>
        '当前设备不支持中文语音识别',
      'error_busy' || 'error_client' || 'error_server' || 'error_server_disconnected' =>
        '语音识别服务暂不可用，请稍后重试',
      final value when value != null && value.isNotEmpty => '语音识别失败：$value',
      _ => null,
    };
  }
}
