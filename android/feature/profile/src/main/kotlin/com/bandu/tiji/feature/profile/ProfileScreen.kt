package com.bandu.tiji.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
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
            remoteAdbDebug = uiState.remoteAdbDebug,
            onBack = { onAction(ProfileAction.Back) },
            onAction = onAction,
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
                        avatar = uiState.avatar,
                        modelConfig = uiState.modelConfigLabel(),
                        deviceName = uiState.deviceName.ifBlank { "未设置" },
                        onSelectAvatarBackground = { index ->
                            onAction(ProfileAction.SelectAvatarBackground(index))
                        },
                        onPickAvatarImage = {
                            onAction(ProfileAction.RequestAvatarImagePicker)
                        },
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
    avatar: ProfileAvatarUiState,
    modelConfig: String,
    deviceName: String,
    onSelectAvatarBackground: (Int) -> Unit,
    onPickAvatarImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAvatarActions by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(vertical = BanduSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarButton(
            summary = summary,
            avatar = avatar,
            onClick = { showAvatarActions = true },
        )
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
    if (showAvatarActions) {
        AvatarChoicePanel(
            avatar = avatar,
            onDismissRequest = { showAvatarActions = false },
            onPickAvatarImage = {
                showAvatarActions = false
                onPickAvatarImage()
            },
            onSelectAvatarBackground = { index ->
                showAvatarActions = false
                onSelectAvatarBackground(index)
            },
        )
    }
}

@Composable
private fun AvatarChoicePanel(
    avatar: ProfileAvatarUiState,
    onDismissRequest: () -> Unit,
    onPickAvatarImage: () -> Unit,
    onSelectAvatarBackground: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(BanduSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            ) {
                Text(
                    text = "头像",
                    style = MaterialTheme.typography.titleLarge,
                )
                AvatarChoiceRows(
                    selectedBackgroundIndex = avatar.backgroundIndex
                        .floorMod(AvatarBackgroundColors.size)
                        .takeIf { avatar.imageUri == null },
                    onPickAvatarImage = onPickAvatarImage,
                    onSelectAvatarBackground = onSelectAvatarBackground,
                )
            }
        }
    }
}

@Composable
private fun AvatarChoiceRows(
    selectedBackgroundIndex: Int?,
    onPickAvatarImage: () -> Unit,
    onSelectAvatarBackground: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageAvatarChoice(onClick = onPickAvatarImage)
            AvatarBackgroundColors.take(3).forEachIndexed { index, color ->
                TextAvatarColorChoice(
                    index = index,
                    color = color,
                    selected = selectedBackgroundIndex == index,
                    onClick = { onSelectAvatarBackground(index) },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBackgroundColors.drop(3).forEachIndexed { offset, color ->
                val index = offset + 3
                TextAvatarColorChoice(
                    index = index,
                    color = color,
                    selected = selectedBackgroundIndex == index,
                    onClick = { onSelectAvatarBackground(index) },
                )
            }
        }
    }
}

@Composable
private fun ImageAvatarChoice(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = BanduBorders.Standard,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "拍照或选择图片头像" }
            .testTag("profile-avatar-image-choice"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun TextAvatarColorChoice(
    index: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else BanduBorders.Standard,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "文字头像背景颜色 ${index + 1}" }
            .testTag("profile-avatar-color-$index"),
    )
}

@Composable
private fun AvatarButton(
    summary: StudentProfileSummary,
    avatar: ProfileAvatarUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                color = AvatarBackgroundColors[
                    avatar.backgroundIndex.floorMod(AvatarBackgroundColors.size)
                ],
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("profile-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar.imageUri == null) {
            Text(
                text = summary.avatarText(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
        } else {
            AsyncImage(
                model = avatar.imageUri,
                contentDescription = "头像图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
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

private val AvatarBackgroundColors = listOf(
    Color(0xFF222222),
    Color(0xFF2F6F6D),
    Color(0xFF8A5A44),
    Color(0xFF6B4E9B),
    Color(0xFFB04A5A),
    Color(0xFF4E6FA8),
)

private fun Int.floorMod(divisor: Int): Int =
    ((this % divisor) + divisor) % divisor
