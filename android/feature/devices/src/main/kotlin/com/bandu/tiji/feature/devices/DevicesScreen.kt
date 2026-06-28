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
                uiState.pairingConfirmation?.let { confirmation ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(PAIRING_CONFIRMATION_TAG),
                        ) {
                            Text("确认设备身份", style = MaterialTheme.typography.titleMedium)
                            Text("对方设备：${confirmation.deviceName}")
                            Text("本机身份：${confirmation.localFingerprint}")
                            Text("对方身份：${confirmation.peerFingerprint}")
                            Text("确认后才保留本次配对关系。")
                            confirmation.errorMessage?.let { Text(it) }
                            Button(
                                onClick = { onAction(DevicesAction.ConfirmPairingIdentity) },
                                enabled = !confirmation.isRejecting,
                                modifier = Modifier.testTag(PAIRING_CONFIRM_TAG),
                            ) {
                                Text("身份一致，保留配对")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.RejectPairingIdentity) },
                                enabled = !confirmation.isRejecting,
                                modifier = Modifier.testTag(PAIRING_REJECT_TAG),
                            ) {
                                Text(if (confirmation.isRejecting) "正在取消" else "身份不一致，取消配对")
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
                            Button(
                                onClick = { onAction(DevicesAction.RequestSendAllData(device)) },
                                modifier = Modifier.testTag("send-device-${device.deviceId}"),
                            ) {
                                Text("发送全部数据")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.RequestForgetDevice(device)) },
                                modifier = Modifier.testTag("forget-device-${device.deviceId}"),
                            ) {
                                Text("解除配对")
                            }
                        }
                    }
                }
                uiState.sendConfirmation?.let { pending ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(SEND_CONFIRMATION_TAG),
                        ) {
                            Text("发送全部数据", style = MaterialTheme.typography.titleMedium)
                            Text("目标设备：${pending.target.displayName}")
                            Text("这是完整复制，不是同步。")
                            Text("源设备的数据会保留，不会被删除。")
                            Text("目标设备现有学习数据会被完整替换，不会与源数据合并。")
                            pending.errorMessage?.let { Text(it) }
                            Button(
                                onClick = { onAction(DevicesAction.ConfirmSendAllData) },
                                enabled = !pending.isSending,
                                modifier = Modifier.testTag(SEND_CONFIRM_TAG),
                            ) {
                                Text(if (pending.isSending) "发送中" else "确认发送")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.DismissSendAllData) },
                                enabled = !pending.isSending,
                            ) {
                                Text("取消")
                            }
                        }
                    }
                }
                uiState.forgetDevice?.let { pending ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(FORGET_DEVICE_CONFIRMATION_TAG),
                        ) {
                            Text("解除配对", style = MaterialTheme.typography.titleMedium)
                            Text("确定解除与 ${pending.device.displayName} 的配对吗？")
                            Text("解除后需要重新输入 6 位配对码才能迁移。")
                            pending.errorMessage?.let { Text(it) }
                            Button(
                                onClick = { onAction(DevicesAction.ConfirmForgetDevice) },
                                enabled = !pending.isForgetting,
                                modifier = Modifier.testTag(FORGET_DEVICE_CONFIRM_TAG),
                            ) {
                                Text(if (pending.isForgetting) "解除中" else "确认解除")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.DismissForgetDevice) },
                                enabled = !pending.isForgetting,
                            ) {
                                Text("取消")
                            }
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
internal const val PAIRING_CONFIRMATION_TAG = "devices-pairing-confirmation"
internal const val PAIRING_CONFIRM_TAG = "devices-pairing-confirm"
internal const val PAIRING_REJECT_TAG = "devices-pairing-reject"
internal const val FORGET_DEVICE_CONFIRMATION_TAG = "devices-forget-confirmation"
internal const val FORGET_DEVICE_CONFIRM_TAG = "devices-forget-confirm"
internal const val SEND_CONFIRMATION_TAG = "devices-send-confirmation"
internal const val SEND_CONFIRM_TAG = "devices-send-confirm"

@Composable
private fun DeviceSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}
