package com.bandu.tiji.feature.tutor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TutorSessionsScreen(
    uiState: TutorSessionsUiState,
    onAction: (TutorSessionsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "AI辅导",
        modifier = modifier,
    ) { padding ->
        TutorSessionsContent(uiState, onAction, padding)
    }
}

@Composable
private fun TutorSessionsContent(
    uiState: TutorSessionsUiState,
    onAction: (TutorSessionsAction) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(BanduSpacing.PageHorizontal),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        when {
            uiState.isLoading -> item {
                Text("正在加载会话")
            }
            uiState.sessions.isEmpty() -> item {
                Text("暂无辅导会话")
            }
            else -> items(
                items = uiState.sessions,
                key = { it.id.value },
            ) { session ->
                TutorSessionSummaryCard(
                    session = session,
                    onClick = {
                        onAction(TutorSessionsAction.OpenSession(session.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun TutorSessionSummaryCard(
    session: TutorSessionSummary,
    onClick: () -> Unit,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = session.title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (session.errorItemId == null) {
                "未绑定错题"
            } else {
                "已绑定错题"
            },
        )
        Text(
            text = "更新于 ${formatTutorTimestamp(session.updatedAtEpochMillis)}",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun formatTutorTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TUTOR_TIME_FORMATTER)

private val TUTOR_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
fun TutorSessionScreen(
    uiState: TutorSessionUiState,
    onAction: (TutorSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = uiState.session?.title ?: "辅导会话",
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(BanduSpacing.PageHorizontal),
        ) {
            Text(
                text = when {
                    uiState.isLoading -> "正在加载会话"
                    uiState.session == null -> "会话不存在"
                    else -> "消息 ${uiState.session.messages.size}"
                },
            )
        }
    }
}
