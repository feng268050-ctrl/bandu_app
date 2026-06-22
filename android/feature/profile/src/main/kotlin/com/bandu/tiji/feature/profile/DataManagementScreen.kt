package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
internal fun DataManagementScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "数据管理",
        modifier = modifier,
        navigation = { ProfileBackButton(onAction) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            Text("清除学习数据", style = MaterialTheme.typography.titleMedium)
            Text(
                "删除题集、错题、图片、标签、统计、辅导会话和练习。" +
                    "保留学生资料、AI 配置/API Key、设备身份和已配对设备。",
            )
            Button(
                onClick = { onAction(ProfileAction.RequestClearLearningData) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("清除学习数据")
            }
            uiState.dataManagement.statusMessage?.let { Text(it) }
        }
    }

    if (uiState.dataManagement.showClearLearningConfirmation) {
        BanduDangerConfirmationDialog(
            title = "永久清除学习数据",
            message = listOfNotNull(
                "此操作不可撤销。学生资料、AI 配置/API Key 和设备身份会保留。",
                uiState.dataManagement.errorMessage,
            ).joinToString("\n"),
            requiredConfirmationText = ProfileViewModel.CLEAR_LEARNING_CONFIRMATION_TEXT,
            confirmationText = uiState.dataManagement.confirmationText,
            onConfirmationTextChange = {
                onAction(ProfileAction.UpdateDataConfirmationText(it))
            },
            confirmLabel = if (uiState.dataManagement.isWorking) "清除中" else "永久清除",
            onConfirm = { onAction(ProfileAction.ConfirmClearLearningData) },
            onDismiss = { onAction(ProfileAction.DismissDataConfirmation) },
        )
    }
}
