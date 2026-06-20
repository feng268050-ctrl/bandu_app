package com.bandu.tiji.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduFilterChip
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId

@Composable
internal fun ErrorItemFilterDialog(
    state: ErrorItemListUiState,
    onAction: (ErrorItemListAction) -> Unit,
) {
    val draft = state.filterDraft ?: return
    AlertDialog(
        onDismissRequest = { onAction(ErrorItemListAction.DismissFilters) },
        title = { Text("筛选错题") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            ) {
                FilterHeading("掌握状态")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    MasteryLevel.entries.forEach { level ->
                        BanduFilterChip(
                            label = level.filterLabel(),
                            selected = level in draft.masteryLevels,
                            onClick = {
                                onAction(ErrorItemListAction.ToggleMasteryFilter(level))
                            },
                        )
                    }
                }
                FilterHeading("创建时间")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    ErrorItemTimeRange.entries.forEach { range ->
                        BanduFilterChip(
                            label = range.filterLabel(),
                            selected = range == draft.timeRange,
                            onClick = {
                                onAction(ErrorItemListAction.SelectTimeRange(range))
                            },
                        )
                    }
                }
                FilterHeading("题集")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    CollectionFilterChip(
                        label = "全部题集",
                        id = null,
                        selectedId = draft.collectionId,
                        onSelect = {
                            onAction(ErrorItemListAction.SelectCollectionFilter(it))
                        },
                    )
                    state.availableCollections.forEach { collection ->
                        CollectionFilterChip(
                            label = collection.name,
                            id = collection.id,
                            selectedId = draft.collectionId,
                            onSelect = {
                                onAction(ErrorItemListAction.SelectCollectionFilter(it))
                            },
                        )
                    }
                }
                FilterHeading("标签")
                if (state.availableTags.isEmpty()) {
                    Text("暂无可选标签")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                        state.availableTags.forEach { tag ->
                            TagFilterChip(
                                label = tag.name,
                                id = tag.id,
                                selectedIds = draft.tagIds,
                                onToggle = {
                                    onAction(ErrorItemListAction.ToggleTagFilter(it))
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.gradeSemester,
                    onValueChange = {
                        onAction(ErrorItemListAction.UpdateGradeSemesterFilter(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("年级/学期") },
                    singleLine = true,
                )
                FilterHeading("试卷等级")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    PaperLevel.entries.forEach { level ->
                        BanduFilterChip(
                            label = level.filterLabel(),
                            selected = level in draft.paperLevels,
                            onClick = {
                                onAction(ErrorItemListAction.TogglePaperLevelFilter(level))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAction(ErrorItemListAction.ApplyFilters) }) {
                Text("应用筛选")
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ErrorItemListAction.ClearFilters) }) {
                Text("清空")
            }
        },
    )
}

@Composable
private fun FilterHeading(text: String) {
    Text(text)
}

@Composable
private fun CollectionFilterChip(
    label: String,
    id: CollectionId?,
    selectedId: CollectionId?,
    onSelect: (CollectionId?) -> Unit,
) {
    BanduFilterChip(
        label = label,
        selected = id == selectedId,
        onClick = { onSelect(id) },
    )
}

@Composable
private fun TagFilterChip(
    label: String,
    id: TagId,
    selectedIds: Set<TagId>,
    onToggle: (TagId) -> Unit,
) {
    BanduFilterChip(
        label = label,
        selected = id in selectedIds,
        onClick = { onToggle(id) },
    )
}

private fun MasteryLevel.filterLabel(): String = when (this) {
    MasteryLevel.NEW -> "未掌握"
    MasteryLevel.REVIEWING -> "复习中"
    MasteryLevel.MASTERED -> "已掌握"
}

private fun ErrorItemTimeRange.filterLabel(): String = when (this) {
    ErrorItemTimeRange.ALL -> "全部时间"
    ErrorItemTimeRange.LAST_7_DAYS -> "最近 7 天"
    ErrorItemTimeRange.LAST_30_DAYS -> "最近 30 天"
}

private fun PaperLevel.filterLabel(): String = when (this) {
    PaperLevel.A -> "A 卷"
    PaperLevel.B -> "B 卷"
    PaperLevel.OTHER -> "其他"
}
