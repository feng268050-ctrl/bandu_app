package com.bandu.tiji

import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.bandu.tiji/device",
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "deviceName" -> result.success(deviceName())
                else -> result.notImplemented()
            }
        }
    }

    private fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        if (model.isEmpty()) {
            return "Android 设备"
        }
        if (manufacturer.isEmpty() || model.startsWith(manufacturer, ignoreCase = true)) {
            return model
        }
        return "$manufacturer $model"
    }
}
