package com.bandu.tiji.feature.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
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
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BanduSpacing.PageHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.query.keyword,
                    onValueChange = { onAction(ErrorItemListAction.UpdateKeyword(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("error-item-search"),
                    label = { Text("搜索题目或解析") },
                    singleLine = true,
                )
                TextButton(onClick = { onAction(ErrorItemListAction.OpenFilters) }) {
                    Text(if (uiState.activeFilters.isEmpty) "筛选" else "筛选（已启用）")
                }
            }
            if (uiState.selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BanduSpacing.PageHorizontal),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选择 ${uiState.selectedIds.size} 项")
                    TextButton(
                        onClick = {
                            onAction(
                                ErrorItemListAction.SelectAllLoaded(
                                    items.itemSnapshotList.items.map { it.id }.toSet(),
                                ),
                            )
                        },
                    ) {
                        Text("全选已加载")
                    }
                    TextButton(onClick = { onAction(ErrorItemListAction.ClearSelection) }) {
                        Text("全不选")
                    }
                    TextButton(onClick = { onAction(ErrorItemListAction.RequestBulkDelete) }) {
                        Text("批量删除")
                    }
                }
            }
            when (val refresh = items.loadState.refresh) {
                is LoadState.Error -> BanduErrorState(
                    message = "无法加载错题",
                    onRetry = items::retry,
                    modifier = Modifier.weight(1f),
                )
                LoadState.Loading -> BanduPagingLoadingItem(
                    message = "正在加载错题",
                    modifier = Modifier.weight(1f),
                )
                is LoadState.NotLoading -> {
                    if (items.itemCount == 0) {
                        BanduEmptyState(
                            title = "没有符合条件的错题",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = BanduSpacing.PageHorizontal)
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
                                        selected = item.id in uiState.selectedIds,
                                        selectionMode = uiState.selectedIds.isNotEmpty(),
                                        onClick = {
                                            if (uiState.selectedIds.isEmpty()) {
                                                onAction(ErrorItemListAction.OpenErrorItem(item.id))
                                            } else {
                                                onAction(ErrorItemListAction.ToggleSelection(item.id))
                                            }
                                        },
                                        onLongClick = {
                                            onAction(ErrorItemListAction.ToggleSelection(item.id))
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
    ErrorItemFilterDialog(
        state = uiState,
        onAction = onAction,
    )
    uiState.bulkDelete?.let { pending ->
        BanduDangerConfirmationDialog(
            title = "批量删除错题",
            message = listOfNotNull(
                "确定永久删除选中的 ${pending.ids.size} 道错题吗？",
                pending.errorMessage,
            ).joinToString("\n"),
            confirmLabel = if (pending.isDeleting) "删除中" else "确认删除",
            onConfirm = { onAction(ErrorItemListAction.ConfirmBulkDelete) },
            onDismiss = { onAction(ErrorItemListAction.DismissBulkDelete) },
        )
    }
}

@Composable
internal fun ErrorItemCard(
    item: ErrorItemSummary,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    thumbnailModel: (String) -> Any?,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error-item-${item.id.value}")
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (selectionMode || selected) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onLongClick() },
            )
        }
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
