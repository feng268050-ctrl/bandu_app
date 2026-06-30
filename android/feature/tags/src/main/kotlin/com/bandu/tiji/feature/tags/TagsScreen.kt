package com.bandu.tiji.feature.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.component.BanduBackNavigation
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduEmptyState
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduFilterChip
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode

@Composable
fun TagsScreen(
    uiState: TagsUiState,
    onAction: (TagsAction) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    BanduPageScaffold(
        title = "标签",
        modifier = modifier,
        navigation = onBack?.let { back ->
            { BanduBackNavigation(onBack = back) }
        },
        actions = {
            TextButton(
                onClick = { onAction(TagsAction.OpenCreate(parentId = null)) },
                modifier = Modifier.testTag("tags-create-root"),
            ) {
                Text("新建标签")
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            SubjectSelector(
                subjects = uiState.subjects,
                selectedSubject = uiState.selectedSubject,
                onSelect = { onAction(TagsAction.SelectSubject(it)) },
            )
            when {
                uiState.isLoading -> BanduLoadingState(message = "正在加载标签")
                uiState.loadErrorMessage != null -> BanduErrorState(
                    message = uiState.loadErrorMessage,
                    onRetry = { onAction(TagsAction.Retry) },
                )
                uiState.tree.isEmpty() -> BanduEmptyState(
                    title = "该学科暂无标签",
                    description = "可以创建自定义标签整理错题",
                )
                else -> TagTree(
                    visibleNodes = flattenVisibleTags(
                        tree = uiState.tree,
                        expandedTagIds = uiState.expandedTagIds,
                    ),
                    onAction = onAction,
                )
            }
        }
    }
    uiState.editor?.let { editor ->
        TagEditorDialog(
            state = editor,
            onNameChange = { onAction(TagsAction.EditorNameChanged(it)) },
            onConfirm = { onAction(TagsAction.SubmitEditor) },
            onDismiss = { onAction(TagsAction.DismissEditor) },
        )
    }
    uiState.deleteConfirmation?.let { confirmation ->
        TagDeleteDialog(
            state = confirmation,
            onConfirm = { onAction(TagsAction.ConfirmDelete) },
            onDismiss = { onAction(TagsAction.DismissDelete) },
        )
    }
}

@Composable
private fun SubjectSelector(
    subjects: List<String>,
    selectedSubject: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = BanduSpacing.PageHorizontal,
                vertical = BanduSpacing.Small,
            )
            .testTag("tags-subjects"),
        horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        items(subjects, key = { it }) { subject ->
            BanduFilterChip(
                label = subject,
                selected = subject == selectedSubject,
                onClick = { onSelect(subject) },
                modifier = Modifier.testTag("tags-subject-$subject"),
            )
        }
    }
}

@Composable
private fun TagTree(
    visibleNodes: List<VisibleTagNode>,
    onAction: (TagsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = BanduSpacing.PageHorizontal)
            .testTag("tags-tree"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        items(
            items = visibleNodes,
            key = { it.node.tag.id.value },
        ) { visibleNode ->
            TagTreeRow(
                visibleNode = visibleNode,
                onToggle = {
                    onAction(TagsAction.ToggleExpanded(visibleNode.node.tag.id))
                },
                onOpen = {
                    onAction(TagsAction.OpenTag(visibleNode.node.tag.id))
                },
                onCreate = {
                    onAction(TagsAction.OpenCreate(visibleNode.node.tag.id))
                },
                onRename = {
                    onAction(TagsAction.OpenRename(visibleNode.node))
                },
                onDelete = {
                    onAction(TagsAction.OpenDelete(visibleNode.node))
                },
            )
        }
    }
}

@Composable
private fun TagTreeRow(
    visibleNode: VisibleTagNode,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onCreate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (visibleNode.depth * 16).dp)
            .testTag("tags-node-${visibleNode.node.tag.id.value}")
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall),
            ) {
                Text(
                    text = visibleNode.node.tag.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "第 ${visibleNode.depth + 1} 层 · " +
                        if (visibleNode.node.tag.isSystem) "标准标签" else "自定义标签",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                visibleNode.node.code?.let { code ->
                    Text(
                        text = "编码：$code",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "关联错题：${visibleNode.node.linkedErrorItemCount} 道",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (visibleNode.hasChildren) {
                TextButton(
                    onClick = onToggle,
                    modifier = Modifier.testTag(
                        "tags-toggle-${visibleNode.node.tag.id.value}",
                    ),
                ) {
                    Text(if (visibleNode.isExpanded) "收起" else "展开")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onCreate,
                modifier = Modifier.testTag(
                    "tags-create-child-${visibleNode.node.tag.id.value}",
                ),
            ) {
                Text("新建子标签")
            }
        }
        if (!visibleNode.node.tag.isSystem) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.testTag(
                        "tags-rename-${visibleNode.node.tag.id.value}",
                    ),
                ) {
                    Text("改名")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(
                        "tags-delete-${visibleNode.node.tag.id.value}",
                    ),
                ) {
                    Text("删除")
                }
            }
        }
    }
}

internal data class VisibleTagNode(
    val node: TagNode,
    val depth: Int,
    val hasChildren: Boolean,
    val isExpanded: Boolean,
)

internal fun flattenVisibleTags(
    tree: List<TagNode>,
    expandedTagIds: Set<TagId>,
): List<VisibleTagNode> = buildList {
    fun addVisible(nodes: List<TagNode>, depth: Int) {
        nodes.forEach { node ->
            val expanded = node.tag.id in expandedTagIds
            add(
                VisibleTagNode(
                    node = node,
                    depth = depth,
                    hasChildren = node.children.isNotEmpty(),
                    isExpanded = expanded,
                ),
            )
            if (expanded) addVisible(node.children, depth + 1)
        }
    }
    addVisible(tree, 0)
}
