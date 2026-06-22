package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.AiProviderType

@Composable
internal fun AiConfigurationScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "AI 配置",
        modifier = modifier,
        navigation = { ProfileBackButton(onAction) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            Text("服务类型")
            AiProviderType.entries.forEach { provider ->
                FilterChip(
                    selected = uiState.aiDraft.providerType == provider,
                    onClick = { onAction(ProfileAction.SelectAiProvider(provider)) },
                    label = { Text(provider.displayLabel()) },
                )
            }
            OutlinedTextField(
                value = uiState.aiDraft.displayName,
                onValueChange = { onAction(ProfileAction.UpdateAiDisplayName(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("显示名称") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.aiDraft.baseUrl,
                onValueChange = { onAction(ProfileAction.UpdateAiBaseUrl(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.aiDraft.apiKeyInput,
                onValueChange = { onAction(ProfileAction.UpdateAiApiKey(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = {
                    Text(
                        if (uiState.aiDraft.hasSavedApiKey) {
                            "已安全保存，输入新值可替换"
                        } else {
                            "请输入 API Key"
                        },
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            if (uiState.aiDraft.hasSavedApiKey) {
                Text("已保存：••••••••")
            }
            OutlinedTextField(
                value = uiState.aiDraft.analysisModel,
                onValueChange = { onAction(ProfileAction.UpdateAnalysisModel(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("图片分析模型") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.aiDraft.tutorModel,
                onValueChange = { onAction(ProfileAction.UpdateTutorModel(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("辅导模型") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("允许私有局域网 HTTP")
                Switch(
                    checked = uiState.aiDraft.allowPrivateCleartext,
                    onCheckedChange = { onAction(ProfileAction.RequestPrivateHttp(it)) },
                )
            }
            Text("仅用于明确确认的局域网私有地址；公网 HTTP 始终拒绝。")
            Button(
                onClick = { onAction(ProfileAction.SaveAiConfiguration) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isValidatingAi,
            ) {
                Text(if (uiState.isValidatingAi) "正在验证" else "保存并验证")
            }
            uiState.aiValidationMessage?.let { Text(it) }
        }
    }
    if (uiState.showPrivateHttpRiskConfirmation) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.DismissPrivateHttp) },
            title = { Text("启用私有 HTTP") },
            text = {
                Text("HTTP 不提供传输加密。仅在你信任的局域网内使用，公网地址仍会被拒绝。")
            },
            confirmButton = {
                Button(onClick = { onAction(ProfileAction.ConfirmPrivateHttp) }) {
                    Text("理解风险并启用")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.DismissPrivateHttp) }) {
                    Text("取消")
                }
            },
        )
    }
}

internal fun AiProviderType.displayLabel(): String =
    when (this) {
        AiProviderType.GEMINI -> "Gemini"
        AiProviderType.OPENAI_COMPATIBLE -> "OpenAI-compatible"
    }
