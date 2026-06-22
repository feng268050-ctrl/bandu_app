package com.bandu.tiji.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduFilterChip
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduMasteryBadge
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ErrorItemDetailScreen(
    uiState: ErrorItemDetailUiState,
    onAction: (ErrorItemDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    imageModel: (String) -> Any? = { it },
) {
    BanduPageScaffold(
        title = "错题详情",
        modifier = modifier,
        actions = {
            if (uiState.item != null) {
                TextButton(onClick = { onAction(ErrorItemDetailAction.OpenEditor) }) {
                    Text("编辑")
                }
            }
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = "正在加载错题详情",
                modifier = Modifier.padding(contentPadding),
            )
            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(ErrorItemDetailAction.Retry) },
                modifier = Modifier.padding(contentPadding),
            )
            uiState.item != null -> ErrorItemDetailContent(
                item = uiState.item,
                uiState = uiState,
                onAction = onAction,
                imageModel = imageModel,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
    ErrorItemEditorDialog(
        state = uiState,
        onAction = onAction,
    )
    uiState.pendingDelete?.let { pending ->
        BanduDangerConfirmationDialog(
            title = "删除错题",
            message = listOfNotNull(
                "删除后将立即从题集中消失，首版不提供回收站。",
                pending.errorMessage,
            ).joinToString("\n"),
            confirmLabel = if (pending.isDeleting) "删除中" else "确认删除",
            onConfirm = { onAction(ErrorItemDetailAction.ConfirmDelete) },
            onDismiss = { onAction(ErrorItemDetailAction.DismissDelete) },
        )
    }
}

@Composable
private fun ErrorItemDetailContent(
    item: ErrorItem,
    uiState: ErrorItemDetailUiState,
    onAction: (ErrorItemDetailAction) -> Unit,
    imageModel: (String) -> Any?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = BanduSpacing.PageHorizontal)
            .testTag("error-item-detail-list"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        item.image?.let { image ->
            item {
                AsyncImage(
                    model = imageModel(image.relativePath),
                    contentDescription = "错题原图",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                )
            }
        }
        item { DetailSection("题目", item.questionText) }
        item { DetailSection("答案", item.answerText) }
        item { DetailSection("解析", item.analysis) }
        item {
            DetailSection(
                "错误信息",
                buildString {
                    append(mistakeStatusLabel(item.mistakeStatus))
                    if (item.wrongAnswerText.isNotBlank()) {
                        append("\n错误答案：")
                        append(item.wrongAnswerText)
                    }
                    if (item.mistakeAnalysis.isNotBlank()) {
                        append("\n错误分析：")
                        append(item.mistakeAnalysis)
                    }
                },
            )
        }
        item {
            DetailSection(
                "标签",
                item.tags.joinToString(" · ") { it.name }.ifEmpty { "无标签" },
            )
        }
        item {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("元数据", style = MaterialTheme.typography.titleMedium)
                Text("学科：${item.subject}")
                Text("年级/学期：${item.gradeSemester ?: "未设置"}")
                Text("试卷等级：${paperLevelLabel(item.paperLevel)}")
                Text("创建时间：${formatDetailTime(item.createdAtEpochMillis)}")
                Text("更新时间：${formatDetailTime(item.updatedAtEpochMillis)}")
                BanduMasteryBadge(item.masteryLevel)
            }
        }
        item { DetailSection("笔记", item.notes.ifBlank { "无笔记" }) }
        item {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("掌握状态", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    MasteryLevel.entries.forEach { level ->
                        BanduFilterChip(
                            label = masteryLevelLabel(level),
                            selected = item.masteryLevel == level,
                            onClick = {
                                onAction(ErrorItemDetailAction.UpdateMastery(level))
                            },
                        )
                    }
                }
                if (uiState.isUpdatingMastery) {
                    Text("正在更新掌握状态")
                }
                uiState.masteryErrorMessage?.let { Text(it) }
                TextButton(onClick = { onAction(ErrorItemDetailAction.OpenTutor) }) {
                    Text("AI 辅导")
                }
                TextButton(onClick = { onAction(ErrorItemDetailAction.RequestDelete) }) {
                    Text("删除错题")
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: String) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(content, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun mistakeStatusLabel(status: MistakeStatus): String = when (status) {
    MistakeStatus.WRONG_ATTEMPT -> "状态：已作答但答错"
    MistakeStatus.NOT_ATTEMPTED -> "状态：未作答"
    MistakeStatus.UNKNOWN -> "状态：未标记"
}

private fun paperLevelLabel(level: PaperLevel?): String = when (level) {
    PaperLevel.A -> "A"
    PaperLevel.B -> "B"
    PaperLevel.OTHER -> "其他"
    null -> "未设置"
}

private fun masteryLevelLabel(level: MasteryLevel): String = when (level) {
    MasteryLevel.NEW -> "未掌握"
    MasteryLevel.REVIEWING -> "复习中"
    MasteryLevel.MASTERED -> "已掌握"
}

internal fun formatDetailTime(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DetailTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val DetailTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
