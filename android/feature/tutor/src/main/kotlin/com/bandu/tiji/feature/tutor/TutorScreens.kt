package com.bandu.tiji.feature.tutor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(BanduSpacing.PageHorizontal),
    ) {
        Text(
            text = when {
                uiState.isLoading -> "正在加载会话"
                uiState.sessions.isEmpty() -> "暂无辅导会话"
                else -> "辅导会话 ${uiState.sessions.size}"
            },
        )
    }
}

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
