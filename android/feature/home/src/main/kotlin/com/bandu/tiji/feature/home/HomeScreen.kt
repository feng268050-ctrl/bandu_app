package com.bandu.tiji.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.navigation.NavigationIntent

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "伴读题集",
        modifier = modifier,
    ) { contentPadding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = "正在加载学生资料",
                modifier = Modifier.padding(contentPadding),
            )

            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(HomeAction.RetryProfile) },
                modifier = Modifier.padding(contentPadding),
            )

            else -> HomeContent(
                nickname = uiState.nickname,
                onOpenDestination = {
                    onAction(HomeAction.OpenDestination(it))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(
                        horizontal = BanduSpacing.PageHorizontal,
                        vertical = BanduSpacing.Large,
                    ),
            )
        }
    }
}

@Composable
private fun HomeContent(
    nickname: String?,
    onOpenDestination: (NavigationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            BanduSpacing.CardGap,
        ),
    ) {
        item {
            Text(
                text = nickname?.let { "你好，$it" } ?: "欢迎使用伴读题集",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(
                text = "整理错题，持续复习",
                modifier = Modifier.padding(bottom = BanduSpacing.Small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        items(HomeCards, key = HomeCard::title) { card ->
            BanduCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(card.testTag)
                    .clickable(
                        role = Role.Button,
                        onClick = { onOpenDestination(card.intent) },
                    ),
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = card.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private data class HomeCard(
    val title: String,
    val description: String,
    val testTag: String,
    val intent: NavigationIntent,
)

private val HomeCards = listOf(
    HomeCard(
        title = "上传/拍摄错题",
        description = "通过拍照或相册整理新错题",
        testTag = "home-card-capture",
        intent = NavigationIntent.OpenCapture,
    ),
    HomeCard(
        title = "查看题集",
        description = "浏览题集和已保存错题",
        testTag = "home-card-library",
        intent = NavigationIntent.OpenLibrary,
    ),
    HomeCard(
        title = "PDF 题库",
        description = "导入 PDF 后随机抽题练习",
        testTag = "home-card-question-bank",
        intent = NavigationIntent.OpenQuestionBanks,
    ),
    HomeCard(
        title = "标签",
        description = "管理错题分类标签",
        testTag = "home-card-tags",
        intent = NavigationIntent.OpenTags,
    ),
    HomeCard(
        title = "统计",
        description = "查看学习与复习进度",
        testTag = "home-card-stats",
        intent = NavigationIntent.OpenStats,
    ),
)
