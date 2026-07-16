import 'package:flutter_riverpod/flutter_riverpod.dart';

final networkActivityTrackerProvider = Provider<NetworkActivityTracker>((ref) {
  return NetworkActivityTracker();
});

class NetworkActivityTracker {
  DateTime? _lastRequestAt;

  DateTime? get lastRequestAt => _lastRequestAt;

  void recordRequest() {
    _lastRequestAt = DateTime.now();
  }
}
