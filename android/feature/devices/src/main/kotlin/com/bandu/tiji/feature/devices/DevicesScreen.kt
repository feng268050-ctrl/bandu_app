package com.bandu.tiji.feature.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduEmptyState
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun DevicesScreen(
    uiState: DevicesUiState,
    onAction: (DevicesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "设备",
        modifier = modifier,
    ) { contentPadding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = "正在加载设备信息",
                modifier = Modifier.padding(contentPadding),
            )
            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(DevicesAction.Retry) },
                modifier = Modifier.padding(contentPadding),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = BanduSpacing.PageHorizontal)
                    .testTag("devices-list"),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            ) {
                item {
                    BanduCard(modifier = Modifier.fillMaxWidth()) {
                        Text("本机", style = MaterialTheme.typography.titleMedium)
                        Text(uiState.localDeviceName)
                        Text("身份指纹：${uiState.localFingerprint}")
                        if (uiState.receiveMode == null) {
                            Button(onClick = { onAction(DevicesAction.StartReceiveMode) }) {
                                Text("接收数据")
                            }
                        } else {
                            TextButton(onClick = { onAction(DevicesAction.StopReceiveMode) }) {
                                Text("退出接收模式")
                            }
                        }
                    }
                }
                uiState.receiveMode?.let { receiveMode ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receive-mode-card"),
                        ) {
                            Text("接收模式", style = MaterialTheme.typography.titleMedium)
                            when {
                                receiveMode.isCreating -> Text("正在生成配对码")
                                receiveMode.errorMessage != null ->
                                    Text(receiveMode.errorMessage)
                                receiveMode.code != null -> {
                                    Text("配对码：${receiveMode.code}")
                                    Text(
                                        if (receiveMode.isExpired) {
                                            "配对码已过期"
                                        } else {
                                            "剩余 ${receiveMode.secondsRemaining} 秒"
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    DeviceSectionTitle("附近设备")
                }
                item {
                    val discovering = uiState.transferState ==
                        com.bandu.tiji.domain.transfer.TransferState.Discovering
                    if (discovering) {
                        TextButton(
                            onClick = { onAction(DevicesAction.StopDiscovery) },
                            enabled = !uiState.isDiscoveryCommandRunning,
                        ) {
                            Text("停止发现")
                        }
                    } else {
                        Button(
                            onClick = { onAction(DevicesAction.StartDiscovery) },
                            enabled = !uiState.isDiscoveryCommandRunning,
                        ) {
                            Text("开始发现")
                        }
                    }
                    uiState.commandErrorMessage?.let { Text(it) }
                }
                if (uiState.nearbyDevices.isEmpty()) {
                    item {
                        BanduEmptyState(
                            title = "未发现附近设备",
                            description = "手动开始发现后，确保两台设备位于同一局域网。",
                        )
                    }
                } else {
                    items(
                        count = uiState.nearbyDevices.size,
                        key = { uiState.nearbyDevices[it].discoveryId },
                    ) { index ->
                        val device = uiState.nearbyDevices[index]
                        BanduCard(modifier = Modifier.fillMaxWidth()) {
                            Text(device.displayName)
                            Text(
                                when (device.mode) {
                                    com.bandu.tiji.domain.transfer.DiscoveryMode.PAIR ->
                                        "可配对设备"
                                    com.bandu.tiji.domain.transfer.DiscoveryMode.TRUSTED ->
                                        "已信任设备在线"
                                },
                            )
                            Button(
                                onClick = { onAction(DevicesAction.SelectNearbyDevice(device)) },
                                modifier = Modifier.testTag("pair-device-${device.discoveryId}"),
                            ) {
                                Text("输入配对码")
                            }
                        }
                    }
                }
                uiState.pairingDialog?.let { pairing ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(PAIRING_DIALOG_TAG),
                        ) {
                            Text("配对 ${pairing.device.displayName}", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = pairing.code,
                                onValueChange = { onAction(DevicesAction.UpdatePairingCode(it)) },
                                label = { Text("6 位配对码") },
                                enabled = !pairing.isSubmitting && !pairing.isLimited,
                                singleLine = true,
                                modifier = Modifier.testTag(PAIRING_CODE_FIELD_TAG),
                            )
                            pairing.errorMessage?.let { Text(it) }
                            Button(
                                onClick = { onAction(DevicesAction.SubmitPairingCode) },
                                enabled = !pairing.isSubmitting && !pairing.isLimited,
                                modifier = Modifier.testTag(PAIRING_SUBMIT_TAG),
                            ) {
                                Text(if (pairing.isSubmitting) "配对中" else "确认配对")
                            }
                            TextButton(onClick = { onAction(DevicesAction.DismissPairing) }) {
                                Text("取消")
                            }
                        }
                    }
                }
                item {
                    DeviceSectionTitle("已配对设备")
                }
                if (uiState.trustedDevices.isEmpty()) {
                    item {
                        Text("暂无已配对设备")
                    }
                } else {
                    items(
                        count = uiState.trustedDevices.size,
                        key = { uiState.trustedDevices[it].deviceId },
                    ) { index ->
                        val device = uiState.trustedDevices[index]
                        BanduCard(modifier = Modifier.fillMaxWidth()) {
                            Text(device.displayName)
                            Text("身份指纹：${device.publicKeyFingerprint}")
                        }
                    }
                }
            }
        }
    }
}

internal const val PAIRING_DIALOG_TAG = "devices-pairing-dialog"
internal const val PAIRING_CODE_FIELD_TAG = "devices-pairing-code"
internal const val PAIRING_SUBMIT_TAG = "devices-pairing-submit"

@Composable
private fun DeviceSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}
