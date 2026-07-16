import 'dart:async';

import 'package:bandu_wrong_notebook/application/features/auth/presentation/auth_controller.dart';
import 'package:bandu_wrong_notebook/application/features/library/presentation/library_controller.dart';
import 'package:bandu_wrong_notebook/application/features/network_diagnostics/network_diagnostics_providers.dart';
import 'package:bandu_wrong_notebook/application/features/pending_tasks/pending_task_providers.dart';
import 'package:bandu_wrong_notebook/application/features/stats/stats_providers.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ClientRecoveryHost extends ConsumerStatefulWidget {
  const ClientRecoveryHost({required this.child, super.key});

  final Widget child;

  @override
  ConsumerState<ClientRecoveryHost> createState() => _ClientRecoveryHostState();
}

class _ClientRecoveryHostState extends ConsumerState<ClientRecoveryHost>
    with WidgetsBindingObserver {
  static const _healthPollInterval = Duration(seconds: 30);

  Timer? _timer;
  bool _isForeground = true;
  bool _isChecking = false;
  bool? _wasHealthy;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _timer = Timer.periodic(
      _healthPollInterval,
      (_) => unawaited(_checkAndRecover()),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_checkAndRecover(retryWhenHealthy: true));
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _isForeground = state == AppLifecycleState.resumed;
    if (_isForeground) {
      unawaited(_checkAndRecover(retryWhenHealthy: true));
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<AuthStatus>(
      authControllerProvider.select((state) => state.status),
      (previous, next) {
        if (next == AuthStatus.signedIn && previous != next) {
          unawaited(_checkAndRecover(retryWhenHealthy: true));
        }
      },
    );
    return widget.child;
  }

  Future<void> _checkAndRecover({bool retryWhenHealthy = false}) async {
    if (!_isForeground || _isChecking || !mounted) {
      return;
    }
    if (ref.read(authControllerProvider).status != AuthStatus.signedIn) {
      return;
    }

    _isChecking = true;
    try {
      final health = await ref.read(checkHealthUseCaseProvider).call();
      final recovered = _wasHealthy == false && health.isHealthy;
      _wasHealthy = health.isHealthy;
      if (!health.isHealthy) {
        return;
      }

      if (recovered) {
        _refreshRemoteData();
      }
      if (retryWhenHealthy || recovered) {
        final summary =
            await ref.read(retryAllPendingTasksUseCaseProvider).call();
        if (summary.succeeded > 0) {
          _refreshRemoteData();
        }
      }
    } catch (_) {
      // Background recovery must never interrupt normal or offline app usage.
    } finally {
      _isChecking = false;
    }
  }

  void _refreshRemoteData() {
    ref.invalidate(libraryControllerProvider);
    ref.invalidate(statsOverviewProvider);
  }
}
