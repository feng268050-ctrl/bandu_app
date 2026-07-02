package com.bandu.tiji.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
internal fun AboutScreen(
    aboutInfo: AboutInfo,
    onBack: () -> Unit,
    remoteAdbDebug: RemoteAdbDebugUiState = RemoteAdbDebugUiState(),
    onAction: (ProfileAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var versionTapCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        onAction(ProfileAction.RefreshRemoteAdbDebug)
    }
    BanduPageScaffold(
        title = "关于",
        modifier = modifier,
        navigation = {
            TextButton(onClick = onBack) {
                Text("返回")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            item { AboutCard("应用名称", aboutInfo.appName) }
            item {
                VersionCard(
                    versionName = aboutInfo.versionName,
                    onVersionClick = {
                        versionTapCount += 1
                        if (versionTapCount >= REMOTE_ADB_SECRET_TAP_COUNT) {
                            versionTapCount = 0
                            onAction(ProfileAction.RequestEnableRemoteAdbDebug)
                        }
                    },
                )
            }
            item { AboutCard("隐私说明", aboutInfo.privacyStatement) }
            item { AboutCard("开源许可", aboutInfo.openSourceLicenses) }
            item { AboutCard("图标来源", aboutInfo.iconSource) }
        }
    }
    if (remoteAdbDebug.showEnableConfirmation) {
        AlertDialog(
            onDismissRequest = {
                onAction(ProfileAction.DismissRemoteAdbDebugConfirmation)
            },
            title = { Text("版本 ${aboutInfo.versionName}") },
            text = {
                RemoteAdbDebugDialogContent(
                    versionName = aboutInfo.versionName,
                    state = remoteAdbDebug,
                )
            },
            confirmButton = {
                if (remoteAdbDebug.isEnabled) {
                    OutlinedButton(
                        onClick = {
                            onAction(ProfileAction.DisableRemoteAdbDebug)
                        },
                        enabled = !remoteAdbDebug.isWorking,
                    ) {
                        Text(if (remoteAdbDebug.isWorking) "处理中" else "关闭调试")
                    }
                } else {
                    Button(
                        onClick = {
                            onAction(ProfileAction.ConfirmEnableRemoteAdbDebug)
                        },
                        enabled = remoteAdbDebug.isSupported && !remoteAdbDebug.isWorking,
                    ) {
                        Text(if (remoteAdbDebug.isWorking) "处理中" else "开启")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(ProfileAction.DismissRemoteAdbDebugConfirmation)
                    },
                    enabled = !remoteAdbDebug.isWorking,
                ) {
                    Text(if (remoteAdbDebug.isEnabled) "完成" else "取消")
                }
            },
        )
    }
}

@Composable
private fun VersionCard(
    versionName: String,
    onVersionClick: () -> Unit,
) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text("版本", style = MaterialTheme.typography.titleMedium)
        Text(
            text = versionName,
            modifier = Modifier
                .testTag("about-version-value")
                .clickable(onClick = onVersionClick),
        )
    }
}

@Composable
private fun AboutCard(title: String, content: String) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(content)
    }
}

@Composable
private fun RemoteAdbDebugDialogContent(
    versionName: String,
    state: RemoteAdbDebugUiState,
) {
    Column(
        modifier = Modifier.testTag("remote-adb-debug-dialog"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        Text("当前版本：$versionName")
        Text("ADB 远程调试", style = MaterialTheme.typography.titleMedium)
        Text(
            text = state.statusText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.connectCommand?.let { command ->
            Text(
                text = command,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.isEnabled && state.connectCommand == null) {
            Text(
                text = "未获取到 Wi-Fi IPv4",
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.statusMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (!state.isEnabled) {
            Text("开启后，同一 Wi-Fi 内可通过 ADB 连接本机。默认 30 分钟后自动关闭。")
        }
    }
}

private fun RemoteAdbDebugUiState.statusText(): String =
    when {
        isEnabled -> {
            val expiresText = expiresAtEpochMillis?.let { "，30 分钟内有效" }.orEmpty()
            "已开启：端口 $port$expiresText"
        }
        isSupported -> "未开启"
        else -> unsupportedReason ?: "当前设备不支持"
    }

private const val REMOTE_ADB_SECRET_TAP_COUNT = 5
