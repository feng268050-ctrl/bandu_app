import 'dart:async';

import 'package:bandu_wrong_notebook/app/app.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();

  FlutterError.onError = (details) {
    FlutterError.presentError(details);
    Zone.current.handleUncaughtError(
      details.exception,
      details.stack ?? StackTrace.current,
    );
  };

  await runZonedGuarded(
    () async => runApp(const ProviderScope(child: BanduApp())),
    (error, stackTrace) {
      if (kDebugMode) {
        debugPrint('Uncaught Flutter error: $error');
        debugPrintStack(stackTrace: stackTrace);
      }
    },
  );
}
