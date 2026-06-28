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
import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.domain.transfer.TransferState

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
                (uiState.transferState as? TransferState.AwaitingOfferConfirmation)?.let { state ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(INCOMING_TRANSFER_CONFIRMATION_TAG),
                        ) {
                            Text("接收数据迁移", style = MaterialTheme.typography.titleMedium)
                            Text("来源设备：${state.offer.sourceDeviceName}")
                            Text("记录数：${state.offer.totalRecords}")
                            Text("文件数：${state.offer.totalFiles}")
                            Text("总大小：${formatBytes(state.offer.totalBytes)}")
                            Text("接收后，本机现有学习数据会被完整替换。")
                            Text("迁移成功后，本机 API Key 会被清除，需要重新配置。")
                            Button(
                                onClick = { onAction(DevicesAction.AcceptIncomingTransfer) },
                                modifier = Modifier.testTag(INCOMING_ACCEPT_TAG),
                            ) {
                                Text("确认接收")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.RejectIncomingTransfer) },
                                modifier = Modifier.testTag(INCOMING_REJECT_TAG),
                            ) {
                                Text("拒绝")
                            }
                        }
                    }
                }
                transferProgressState(uiState.transferState)?.let { progressState ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TRANSFER_STATUS_TAG),
                        ) {
                            Text("迁移状态", style = MaterialTheme.typography.titleMedium)
                            Text("阶段：${progressState.phaseLabel}")
                            Text("总进度：${progressState.percent}%")
                            Text("已传输：${formatBytes(progressState.transferredBytes)} / ${formatBytes(progressState.totalBytes)}")
                            Text("速度：${formatBytes(progressState.bytesPerSecond)}/s")
                            progressState.errorMessage?.let { Text("错误：$it") }
                            progressState.resumable?.let { resumable ->
                                Text(if (resumable) "可续传：是" else "可续传：否")
                            }
                            if (uiState.transferState is TransferState.Failed) {
                                Text("迁移未完成，目标设备原数据仍可用。")
                            } else {
                                TextButton(
                                    onClick = { onAction(DevicesAction.CancelTransfer) },
                                    modifier = Modifier.testTag(TRANSFER_CANCEL_TAG),
                                ) {
                                    Text("取消迁移")
                                }
                            }
                        }
                    }
                }
                if (uiState.transferState is TransferState.AwaitingFinalConfirmation) {
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(FINAL_CONFIRMATION_TAG),
                        ) {
                            Text("最终确认", style = MaterialTheme.typography.titleMedium)
                            Text("双方确认后，目标设备才会切换到新数据。")
                            Text("确认前取消或失败，目标设备原数据仍可用。")
                            Button(
                                onClick = { onAction(DevicesAction.ConfirmFinalTransfer) },
                                modifier = Modifier.testTag(FINAL_CONFIRM_TAG),
                            ) {
                                Text("最终确认")
                            }
                            TextButton(
                                onClick = { onAction(DevicesAction.CancelTransfer) },
                                modifier = Modifier.testTag(FINAL_CANCEL_TAG),
                            ) {
                                Text("取消迁移")
                            }
                        }
                    }
                }
                (uiState.transferState as? TransferState.Completed)?.let { state ->
                    item {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TRANSFER_SUCCESS_TAG),
                        ) {
                            Text("迁移完成", style = MaterialTheme.typography.titleMedium)
                            Text("已传输：${formatBytes(state.summary.transferredBytes)}")
                            Text("耗时：${state.summary.durationMillis / 1_000L} 秒")
                            Text("源设备数据已保留，不会被删除。")
                            Text("目标设备 API Key 已清除，请在“我的”中重新配置 AI 服务。")
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
internal const val INCOMING_TRANSFER_CONFIRMATION_TAG = "devices-incoming-confirmation"
internal const val INCOMING_ACCEPT_TAG = "devices-incoming-accept"
internal const val INCOMING_REJECT_TAG = "devices-incoming-reject"
internal const val TRANSFER_STATUS_TAG = "devices-transfer-status"
internal const val TRANSFER_CANCEL_TAG = "devices-transfer-cancel"
internal const val FINAL_CONFIRMATION_TAG = "devices-final-confirmation"
internal const val FINAL_CONFIRM_TAG = "devices-final-confirm"
internal const val FINAL_CANCEL_TAG = "devices-final-cancel"
internal const val TRANSFER_SUCCESS_TAG = "devices-transfer-success"

@Composable
private fun DeviceSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}

internal fun formatBytes(bytes: Long): String =
    when {
        bytes < 1_024L -> "$bytes B"
        bytes < 1_024L * 1_024L -> "${bytes / 1_024L} KB"
        else -> "${bytes / (1_024L * 1_024L)} MB"
    }

private data class TransferProgressUiState(
    val phaseLabel: String,
    val percent: Int,
    val transferredBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val errorMessage: String? = null,
    val resumable: Boolean? = null,
)

private fun transferProgressState(state: TransferState): TransferProgressUiState? =
    when (state) {
        is TransferState.Transferring -> TransferProgressUiState(
            phaseLabel = state.progress.phase.label(),
            percent = state.progress.percentComplete,
            transferredBytes = state.progress.transferredBytes,
            totalBytes = state.progress.totalBytes,
            bytesPerSecond = state.progress.bytesPerSecond,
        )
        is TransferState.Verifying -> TransferProgressUiState(
            phaseLabel = "校验中",
            percent = state.progress,
            transferredBytes = 0L,
            totalBytes = 0L,
            bytesPerSecond = 0L,
        )
        TransferState.Committing -> TransferProgressUiState(
            phaseLabel = "提交中",
            percent = 100,
            transferredBytes = 0L,
            totalBytes = 0L,
            bytesPerSecond = 0L,
        )
        is TransferState.Failed -> TransferProgressUiState(
            phaseLabel = "失败",
            percent = 0,
            transferredBytes = 0L,
            totalBytes = 0L,
            bytesPerSecond = 0L,
            errorMessage = state.code.errorLabel(),
            resumable = state.resumable,
        )
        else -> null
    }

private fun TransferPhase.label(): String =
    when (this) {
        TransferPhase.PAIRING -> "配对中"
        TransferPhase.OFFER -> "确认中"
        TransferPhase.TRANSFERRING -> "传输中"
        TransferPhase.VERIFYING -> "校验中"
        TransferPhase.COMMITTING -> "提交中"
        TransferPhase.COMPLETED -> "已完成"
        TransferPhase.FAILED -> "失败"
    }

private fun com.bandu.tiji.domain.transfer.TransferFailureCode.errorLabel(): String =
    when (this) {
        com.bandu.tiji.domain.transfer.TransferFailureCode.PAIRING_FAILED -> "配对失败"
        com.bandu.tiji.domain.transfer.TransferFailureCode.PAIRING_EXPIRED -> "配对码过期"
        com.bandu.tiji.domain.transfer.TransferFailureCode.OFFER_REJECTED -> "对方拒绝迁移"
        com.bandu.tiji.domain.transfer.TransferFailureCode.CHECKSUM_FAILED -> "数据校验失败"
        com.bandu.tiji.domain.transfer.TransferFailureCode.NETWORK_INTERRUPTED -> "网络中断"
        com.bandu.tiji.domain.transfer.TransferFailureCode.PROTOCOL_ERROR -> "协议错误"
        com.bandu.tiji.domain.transfer.TransferFailureCode.COMMIT_FAILED -> "提交失败"
        com.bandu.tiji.domain.transfer.TransferFailureCode.CANCELLED -> "已取消"
    }
