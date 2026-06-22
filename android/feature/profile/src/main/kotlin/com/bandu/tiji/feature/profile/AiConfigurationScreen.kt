package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        }
    }
}

internal fun AiProviderType.displayLabel(): String =
    when (this) {
        AiProviderType.GEMINI -> "Gemini"
        AiProviderType.OPENAI_COMPATIBLE -> "OpenAI-compatible"
    }
