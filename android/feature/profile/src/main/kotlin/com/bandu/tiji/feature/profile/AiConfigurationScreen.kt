package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.domain.ai.PromptType

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
                .padding(BanduSpacing.PageHorizontal)
                .verticalScroll(rememberScrollState()),
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
            CompositionLocalProvider(LocalTextToolbar provides DisabledTextToolbar) {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
            }
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
            Text("高级提示词")
            PromptType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.promptEditor.type == type,
                    onClick = { onAction(ProfileAction.SelectPromptType(type)) },
                    label = { Text(type.profileLabel()) },
                )
            }
            OutlinedTextField(
                value = uiState.promptEditor.template,
                onValueChange = { onAction(ProfileAction.UpdatePromptTemplate(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("提示词模板") },
                minLines = 6,
            )
            uiState.promptEditor.errorMessage?.let { Text(it) }
            uiState.promptEditor.statusMessage?.let { Text(it) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            ) {
                Button(
                    onClick = { onAction(ProfileAction.SavePrompt) },
                    enabled = !uiState.promptEditor.isSaving,
                ) {
                    Text("保存提示词")
                }
                TextButton(
                    onClick = { onAction(ProfileAction.ResetPrompt) },
                    enabled = !uiState.promptEditor.isSaving,
                ) {
                    Text("恢复默认")
                }
            }
            TextButton(onClick = { onAction(ProfileAction.RequestAiDataConsent) }) {
                Text("查看 AI 数据发送说明")
            }
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
    if (uiState.showAiDataConsent) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.DismissAiDataConsent) },
            title = { Text("AI 数据发送说明") },
            text = {
                Text(
                    "使用 AI 时，处理后的题目图片、题目内容、会话上下文和你的输入会发送到" +
                        "所选 AI 服务商。API Key 仅保存在本机，不参与设备迁移。",
                )
            },
            confirmButton = {
                Button(onClick = { onAction(ProfileAction.ConfirmAiDataConsent) }) {
                    Text("同意并继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.DismissAiDataConsent) }) {
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

private fun PromptType.profileLabel(): String = when (this) {
    PromptType.ANALYZE_IMAGE -> "图片分析"
    PromptType.TUTOR -> "辅导"
    PromptType.GENERATE_EXERCISE -> "变式题生成"
    PromptType.GRADE_EXERCISE -> "练习批改"
}

private object DisabledTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}
