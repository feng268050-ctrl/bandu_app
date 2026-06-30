package com.bandu.tiji.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduBackNavigation
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
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
    onBack: (() -> Unit)? = null,
) {
    BanduPageScaffold(
        title = "题集",
        modifier = modifier,
        navigation = onBack?.let { back ->
            { BanduBackNavigation(onBack = back) }
        },
        actions = {
            TextButton(onClick = { onAction(CollectionListAction.RequestCreate) }) {
                Text("新建题集")
            }
        },
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
                        TextButton(
                            onClick = {
                                onAction(CollectionListAction.RequestRename(collection.id))
                            },
                        ) {
                            Text("重命名")
                        }
                        TextButton(
                            onClick = {
                                onAction(CollectionListAction.RequestDelete(collection.id))
                            },
                        ) {
                            Text("删除题集")
                        }
                    }
                }
            }
        }
    }

    uiState.editor?.let { editor ->
        CollectionEditorDialog(
            editor = editor,
            onAction = onAction,
        )
    }
    uiState.pendingDelete?.let { pendingDelete ->
        val impact = if (pendingDelete.collection.errorItemCount > 0) {
            "删除“${pendingDelete.collection.name}”将同时永久删除其中的" +
                " ${pendingDelete.collection.errorItemCount} 道错题。"
        } else {
            "确定永久删除空题集“${pendingDelete.collection.name}”吗？"
        }
        BanduDangerConfirmationDialog(
            title = "删除题集",
            message = listOfNotNull(impact, pendingDelete.errorMessage).joinToString("\n"),
            confirmLabel = if (pendingDelete.isDeleting) "删除中" else "确认删除",
            onConfirm = { onAction(CollectionListAction.ConfirmDelete) },
            onDismiss = { onAction(CollectionListAction.DismissDelete) },
        )
    }
}

@Composable
private fun CollectionEditorDialog(
    editor: CollectionEditorUiState,
    onAction: (CollectionListAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(CollectionListAction.DismissEditor) },
        title = {
            Text(
                when (editor.mode) {
                    CollectionEditorMode.Create -> "新建题集"
                    is CollectionEditorMode.Rename -> "重命名题集"
                },
            )
        },
        text = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = {
                    onAction(CollectionListAction.UpdateEditorName(it))
                },
                label = { Text("题集名称") },
                supportingText = editor.errorMessage?.let { message ->
                    { Text(message) }
                },
                isError = editor.errorMessage != null,
                enabled = !editor.isSaving,
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onAction(CollectionListAction.SubmitEditor) },
                enabled = !editor.isSaving,
            ) {
                Text(if (editor.isSaving) "保存中" else "保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(CollectionListAction.DismissEditor) },
                enabled = !editor.isSaving,
            ) {
                Text("取消")
            }
        },
    )
}

internal fun formatCollectionUpdatedAt(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = CollectionDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val CollectionDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
