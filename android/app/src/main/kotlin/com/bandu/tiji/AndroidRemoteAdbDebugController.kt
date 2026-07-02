package com.bandu.tiji

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import com.bandu.tiji.feature.profile.DEFAULT_REMOTE_ADB_PORT
import com.bandu.tiji.feature.profile.RemoteAdbDebugController
import com.bandu.tiji.feature.profile.RemoteAdbDebugOperationResult
import com.bandu.tiji.feature.profile.RemoteAdbDebugRuntimeState
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class AndroidRemoteAdbDebugController(
    context: Context,
    private val shell: DeviceShell = AndroidDeviceShell(),
) : RemoteAdbDebugController {
    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(readState())

    override fun observeState(): Flow<RemoteAdbDebugRuntimeState> = mutableState

    override suspend fun refresh(): RemoteAdbDebugRuntimeState =
        withContext(Dispatchers.IO) {
            readAndPublishState()
        }

    override suspend fun enable(port: Int): RemoteAdbDebugOperationResult =
        withContext(Dispatchers.IO) {
            if (port !in MIN_TCP_PORT..MAX_TCP_PORT) {
                val state = readAndPublishState()
                return@withContext RemoteAdbDebugOperationResult.Failure(
                    message = "ADB 端口必须在 $MIN_TCP_PORT-$MAX_TCP_PORT 之间",
                    state = state,
                )
            }
            val support = evaluateSupport()
            if (!support.isSupported) {
                val state = readAndPublishState(support)
                return@withContext RemoteAdbDebugOperationResult.Unsupported(
                    reason = support.unsupportedReason ?: "当前设备不支持",
                    state = state,
                )
            }
            val adbEnabled = runCatching {
                Settings.Global.putInt(
                    appContext.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    1,
                )
            }.getOrDefault(false)
            if (!adbEnabled) {
                val state = readAndPublishState(support)
                return@withContext RemoteAdbDebugOperationResult.Failure(
                    message = "无法写入 ADB_ENABLED，请确认系统权限已授予",
                    state = state,
                )
            }
            val commands = listOf(
                "setprop service.adb.tcp.port $port",
                "setprop persist.adb.tcp.port $port",
                "stop adbd",
                "start adbd",
            )
            commands.forEach(shell::executeAsRoot)
            val state = readAndPublishState(support)
            if (!state.isEnabled || state.port != port) {
                return@withContext RemoteAdbDebugOperationResult.Failure(
                    message = "无法重启 adbd TCP 端口，请确认设备提供 root/OEM shell 能力",
                    state = state,
                )
            }
            RemoteAdbDebugOperationResult.Success(state)
        }

    override suspend fun disable(): RemoteAdbDebugOperationResult =
        withContext(Dispatchers.IO) {
            val support = evaluateSupport()
            if (!support.isSupported) {
                val state = readAndPublishState(support)
                return@withContext RemoteAdbDebugOperationResult.Unsupported(
                    reason = support.unsupportedReason ?: "当前设备不支持",
                    state = state,
                )
            }
            val commands = listOf(
                "setprop service.adb.tcp.port -1",
                "setprop persist.adb.tcp.port -1",
                "stop adbd",
                "start adbd",
            )
            commands.forEach(shell::executeAsRoot)
            val state = readAndPublishState(support)
            if (state.isEnabled) {
                return@withContext RemoteAdbDebugOperationResult.Failure(
                    message = "无法关闭 adbd TCP 端口，请确认设备提供 root/OEM shell 能力",
                    state = state,
                )
            }
            RemoteAdbDebugOperationResult.Success(state)
        }

    private fun readAndPublishState(
        support: SupportEvaluation = evaluateSupport(),
    ): RemoteAdbDebugRuntimeState {
        val state = readState(support)
        mutableState.value = state
        return state
    }

    private fun readState(
        support: SupportEvaluation = evaluateSupport(),
    ): RemoteAdbDebugRuntimeState {
        val activePort = readAdbTcpPort()
        return RemoteAdbDebugRuntimeState(
            isSupported = support.isSupported,
            isEnabled = activePort != null,
            port = activePort ?: DEFAULT_REMOTE_ADB_PORT,
            ipAddress = resolveWifiIpv4Address(),
            unsupportedReason = support.unsupportedReason,
        )
    }

    private fun evaluateSupport(): SupportEvaluation {
        if (!isSystemOrPrivilegedInstall()) {
            return SupportEvaluation(
                isSupported = false,
                unsupportedReason = "当前不是系统特权安装",
            )
        }
        if (
            appContext.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SupportEvaluation(
                isSupported = false,
                unsupportedReason = "缺少 WRITE_SECURE_SETTINGS 授权",
            )
        }
        return SupportEvaluation(isSupported = true, unsupportedReason = null)
    }

    private fun isSystemOrPrivilegedInstall(): Boolean {
        val info = appContext.applicationInfo
        val sourceDir = info.sourceDir.orEmpty()
        if (sourceDir.startsWith("/system/") || sourceDir.contains("/system/priv-app/")) {
            return true
        }
        return info.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    private fun readAdbTcpPort(): Int? {
        val servicePort = shell.readProperty("service.adb.tcp.port").toAdbPortOrNull()
        if (servicePort != null) return servicePort
        return shell.readProperty("persist.adb.tcp.port").toAdbPortOrNull()
    }

    @Suppress("DEPRECATION")
    private fun resolveWifiIpv4Address(): String? {
        val connectivityManager = appContext.getSystemService(
            ConnectivityManager::class.java,
        )
        val wifiNetworkAddress = connectivityManager
            ?.allNetworks
            ?.asSequence()
            ?.filter { network ->
                connectivityManager.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            ?.mapNotNull { network ->
                connectivityManager.getLinkProperties(network)
                    ?.linkAddresses
                    ?.asSequence()
                    ?.map { it.address }
                    ?.filterIsInstance<Inet4Address>()
                    ?.firstOrNull { !it.isLoopbackAddress }
                    ?.hostAddress
            }
            ?.firstOrNull { it.isNotBlank() }
        if (!wifiNetworkAddress.isNullOrBlank()) return wifiNetworkAddress

        val wifiManager = appContext.getSystemService(WifiManager::class.java)
        val wifiInfoAddress = wifiManager?.connectionInfo?.ipAddress
            ?.takeIf { it != 0 }
            ?.toLittleEndianIpv4()
        if (!wifiInfoAddress.isNullOrBlank()) return wifiInfoAddress

        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) continue
            if (!networkInterface.name.startsWith(WIFI_INTERFACE_PREFIX)) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    return address.hostAddress
                }
            }
        }
        return null
    }

    private data class SupportEvaluation(
        val isSupported: Boolean,
        val unsupportedReason: String?,
    )

    private companion object {
        const val MIN_TCP_PORT = 1024
        const val MAX_TCP_PORT = 65535
        const val WIFI_INTERFACE_PREFIX = "wlan"
    }
}

interface DeviceShell {
    fun executeAsRoot(command: String): Boolean

    fun readProperty(name: String): String?
}

class AndroidDeviceShell : DeviceShell {
    override fun executeAsRoot(command: String): Boolean =
        executeWithInnohiShell(command) == true ||
            runCommand(listOf("su", "0", "sh", "-c", command)).isSuccess ||
            runCommand(listOf("su", "-c", command)).isSuccess ||
            runCommand(listOf("sh", "-c", command)).isSuccess

    override fun readProperty(name: String): String? =
        readSystemProperty(name)
            ?: runCommand(listOf("sh", "-c", "getprop $name")).output.trim()
                .ifBlank { null }

    private fun executeWithInnohiShell(command: String): Boolean? =
        runCatching {
            val klass = Class.forName("com.innohi.ShellCmdUtil")
            val method = klass.getMethod("executeCmd", String::class.java)
            method.invoke(null, command) as? Boolean
        }.getOrNull()

    private fun readSystemProperty(name: String): String? =
        runCatching {
            val klass = Class.forName("android.os.SystemProperties")
            val method = klass.getMethod("get", String::class.java, String::class.java)
            method.invoke(null, name, "") as? String
        }.getOrNull()?.ifBlank { null }

    private fun runCommand(args: List<String>): ShellCommandResult {
        val process = runCatching {
            ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
        }.getOrElse {
            return ShellCommandResult(exitCode = -1, output = it.message.orEmpty())
        }
        val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return ShellCommandResult(exitCode = -1, output = "timeout")
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return ShellCommandResult(exitCode = process.exitValue(), output = output)
    }

    private data class ShellCommandResult(
        val exitCode: Int,
        val output: String,
    ) {
        val isSuccess: Boolean
            get() = exitCode == 0
    }

    private companion object {
        const val COMMAND_TIMEOUT_SECONDS = 5L
    }
}

private fun String?.toAdbPortOrNull(): Int? =
    this
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

private fun Int.toLittleEndianIpv4(): String =
    listOf(
        this and 0xff,
        this shr 8 and 0xff,
        this shr 16 and 0xff,
        this shr 24 and 0xff,
    ).joinToString(".")
