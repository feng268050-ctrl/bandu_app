import 'dart:async';

import 'package:bandu_wrong_notebook/application/app/bandu_app.dart';
import 'package:bandu_wrong_notebook/build/framework_overrides.dart';
import 'package:bandu_wrong_notebook/build/client_recovery_host.dart';
import 'package:bandu_wrong_notebook/framework/config/app_config.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();
  loadAppConfig().validate();

  FlutterError.onError = (details) {
    FlutterError.presentError(details);
    Zone.current.handleUncaughtError(
      details.exception,
      details.stack ?? StackTrace.current,
    );
  };

  await runZonedGuarded(
    () async => runApp(
      ProviderScope(
        overrides: buildFrameworkOverrides(),
        child: const ClientRecoveryHost(child: BanduApp()),
      ),
    ),
    (error, stackTrace) {
      if (kDebugMode) {
        debugPrint('Uncaught Flutter error: $error');
        debugPrintStack(stackTrace: stackTrace);
      }
    },
  );
}
