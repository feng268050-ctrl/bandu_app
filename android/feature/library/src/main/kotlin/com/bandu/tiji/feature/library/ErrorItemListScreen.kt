package com.bandu.tiji.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduEmptyState
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduMasteryBadge
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.component.BanduPagingLoadingItem
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ErrorItemListScreen(
    uiState: ErrorItemListUiState,
    items: LazyPagingItems<ErrorItemSummary>,
    onAction: (ErrorItemListAction) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailModel: (String) -> Any? = { it },
) {
    BanduPageScaffold(
        title = "错题",
        modifier = modifier,
    ) { contentPadding ->
        when (val refresh = items.loadState.refresh) {
            is LoadState.Error -> BanduErrorState(
                message = "无法加载错题",
                onRetry = items::retry,
                modifier = Modifier.padding(contentPadding),
            )
            LoadState.Loading -> BanduPagingLoadingItem(
                message = "正在加载错题",
                modifier = Modifier.padding(contentPadding),
            )
            is LoadState.NotLoading -> {
                if (items.itemCount == 0) {
                    BanduEmptyState(
                        title = "没有符合条件的错题",
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(BanduSpacing.PageHorizontal)
                            .testTag("error-item-list"),
                        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
                    ) {
                        items(
                            count = items.itemCount,
                            key = items.itemKey { it.id.value },
                        ) { index ->
                            items[index]?.let { item ->
                                ErrorItemCard(
                                    item = item,
                                    onClick = {
                                        onAction(ErrorItemListAction.OpenErrorItem(item.id))
                                    },
                                    thumbnailModel = thumbnailModel,
                                )
                            }
                        }
                        if (items.loadState.append == LoadState.Loading) {
                            item { BanduPagingLoadingItem() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ErrorItemCard(
    item: ErrorItemSummary,
    onClick: () -> Unit,
    thumbnailModel: (String) -> Any?,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error-item-${item.id.value}")
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        item.thumbnailPath?.let { path ->
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailModel(path))
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "错题缩略图",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("error-item-thumbnail-${item.id.value}"),
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "无图片",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.questionPreview,
            maxLines = 3,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.collectionName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (item.tags.isNotEmpty()) {
            Text(
                text = item.tags.joinToString(" · ") { it.name },
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
            Text(
                text = formatErrorItemCreatedAt(item.createdAtEpochMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            BanduMasteryBadge(item.masteryLevel)
        }
    }
}

internal fun formatErrorItemCreatedAt(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = ErrorItemDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val ErrorItemDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
