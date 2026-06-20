package com.bandu.tiji.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduEmptyState
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CollectionListScreen(
    uiState: CollectionListUiState,
    onAction: (CollectionListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "题集",
        modifier = modifier,
    ) { contentPadding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = "正在加载题集",
                modifier = Modifier.padding(contentPadding),
            )

            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(CollectionListAction.Retry) },
                modifier = Modifier.padding(contentPadding),
            )

            uiState.collections.isEmpty() -> BanduEmptyState(
                title = "还没有题集",
                description = "新建题集后即可整理错题",
                modifier = Modifier.padding(contentPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(BanduSpacing.PageHorizontal)
                    .testTag("collection-list"),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            ) {
                items(uiState.collections, key = { it.id.value }) { collection ->
                    BanduCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection-${collection.id.value}")
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    onAction(CollectionListAction.OpenCollection(collection.id))
                                },
                            ),
                    ) {
                        Text(
                            text = collection.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${collection.errorItemCount} 道错题",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "最近更新 ${formatCollectionUpdatedAt(collection.updatedAtEpochMillis)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

internal fun formatCollectionUpdatedAt(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = CollectionDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val CollectionDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
