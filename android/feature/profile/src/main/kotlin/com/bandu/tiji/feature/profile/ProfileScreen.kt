package com.bandu.tiji.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduBorders
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import java.util.Locale

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val section = uiState.currentSection) {
        null -> ProfileOverview(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        ProfileSection.STUDENT -> StudentProfileScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        ProfileSection.DEVICE -> DeviceNameScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        ProfileSection.AI -> AiConfigurationScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        ProfileSection.DATA -> DataManagementScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        ProfileSection.ABOUT -> AboutScreen(
            aboutInfo = uiState.aboutInfo,
            onBack = { onAction(ProfileAction.Back) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileOverview(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier,
) {
    BanduPageScaffold(title = "我的", modifier = modifier) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            uiState.studentSummary?.let { summary ->
                item {
                    StudentSummaryHeader(
                        summary = summary,
                        modelConfig = uiState.modelConfigLabel(),
                        deviceName = uiState.deviceName.ifBlank { "未设置" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile-student-summary"),
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BanduBorders.Standard)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            items(ProfileSection.entries, key = ProfileSection::name) { section ->
                BanduCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile-section-${section.name.lowercase()}")
                        .clickable(
                            role = Role.Button,
                            onClick = { onAction(ProfileAction.OpenSection(section)) },
                        ),
                ) {
                    Text(section.title)
                    Text(section.description)
                }
            }
        }
    }
}

@Composable
private fun StudentSummaryHeader(
    summary: StudentProfileSummary,
    modelConfig: String,
    deviceName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(vertical = BanduSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = summary.avatarText(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(BanduSpacing.PageHorizontal))
        Column(
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall),
        ) {
            Text(
                text = summary.nickname.ifBlank { "未设置昵称" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = listOfNotNull(
                    summary.educationStage,
                    summary.grade?.let { "${it}年级" },
                ).ifEmpty { listOf("未设置教育阶段和年级") }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "模型配置：$modelConfig",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "设备名称：$deviceName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun ProfileUiState.modelConfigLabel(): String {
    if (!isAiConfigurationActive) return "未配置"
    val modelNames = listOf(aiDraft.analysisModel, aiDraft.tutorModel)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(" / ")
    return if (modelNames.isBlank()) {
        aiDraft.displayName.ifBlank { "未命名服务" }
    } else {
        "${aiDraft.displayName.ifBlank { "未命名服务" }} · $modelNames"
    }
}

private fun StudentProfileSummary.avatarText(): String =
    nickname.trim()
        .firstOrNull()
        ?.toString()
        ?.uppercase(Locale.getDefault())
        ?: "我"
