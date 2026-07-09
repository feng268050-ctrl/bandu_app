package com.bandu.tiji.core.storage.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.concurrent.TimeUnit

class AndroidDeviceNameResolver(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun resolve(): String {
        val settingsName = readGlobalDeviceName()
        val bluetoothName = readBluetoothName()
        val buildModel = Build.MODEL.orEmpty()
        val emulatorName = readEmulatorAvdName()

        if (emulatorName != null && listOf(settingsName, bluetoothName, buildModel).any(::isGenericSdkName)) {
            return emulatorName
        }

        return listOf(settingsName, bluetoothName, buildModel, Build.DEVICE.orEmpty())
            .firstNotNullOfOrNull(::normalizeDeviceName)
            ?: FALLBACK_DEVICE_NAME
    }

    fun shouldReplaceStoredName(storedName: String): Boolean {
        val resolvedName = resolve()
        val normalizedStoredName = normalizeDeviceName(storedName) ?: return true
        return normalizedStoredName != resolvedName && isGenericSdkName(normalizedStoredName)
    }

    private fun readGlobalDeviceName(): String? =
        Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)

    private fun readBluetoothName(): String? =
        Settings.Secure.getString(appContext.contentResolver, BLUETOOTH_NAME_KEY)

    private fun readEmulatorAvdName(): String? =
        listOf(RO_BOOT_AVD_NAME, QEMU_AVD_NAME)
            .firstNotNullOfOrNull(::readSystemProperty)
            ?.replace('_', ' ')
            ?.let(::normalizeDeviceName)

    private fun readSystemProperty(name: String): String? =
        runCatching {
            val process = ProcessBuilder(GETPROP_COMMAND, name)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(GETPROP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                return null
            }
            process.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()?.let(::normalizeDeviceName)

    companion object {
        private const val FALLBACK_DEVICE_NAME = "Android"
        private const val BLUETOOTH_NAME_KEY = "bluetooth_name"
        private const val GETPROP_COMMAND = "getprop"
        private const val GETPROP_TIMEOUT_MILLIS = 200L
        private const val RO_BOOT_AVD_NAME = "ro.boot.qemu.avd_name"
        private const val QEMU_AVD_NAME = "qemu.avd_name"

        fun normalizeDeviceName(name: String?): String? =
            name
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.takeIf(String::isNotBlank)
                ?.take(MAX_DEVICE_NAME_LENGTH)

        fun isGenericSdkName(name: String?): Boolean {
            val normalized = normalizeDeviceName(name)?.lowercase() ?: return false
            return normalized == "android" ||
                normalized.startsWith("sdk ") ||
                normalized.startsWith("sdk_") ||
                normalized.startsWith("aosp ") ||
                normalized.startsWith("aosp_") ||
                normalized.contains("gphone")
        }

        private const val MAX_DEVICE_NAME_LENGTH = 40
    }
}
