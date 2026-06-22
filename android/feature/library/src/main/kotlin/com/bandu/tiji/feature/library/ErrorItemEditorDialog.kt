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
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.id.TagId

@Composable
internal fun ErrorItemEditorDialog(
    state: ErrorItemDetailUiState,
    onAction: (ErrorItemDetailAction) -> Unit,
) {
    val draft = state.editor ?: return
    fun update(transform: (ErrorItemEditDraft) -> ErrorItemEditDraft) {
        onAction(ErrorItemDetailAction.UpdateEditor(transform(draft)))
    }

    AlertDialog(
        onDismissRequest = { onAction(ErrorItemDetailAction.DismissEditor) },
        title = { Text("编辑错题") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            ) {
                TextButton(
                    onClick = { onAction(ErrorItemDetailAction.RequestImageReplacement) },
                ) {
                    Text(if (draft.image == null) "添加图片" else "更换图片")
                }
                EditorField("题目", draft.questionText) {
                    val value = it
                    update { current -> current.copy(questionText = value) }
                }
                EditorField("答案", draft.answerText) {
                    val value = it
                    update { current -> current.copy(answerText = value) }
                }
                EditorField("解析", draft.analysis) {
                    val value = it
                    update { current -> current.copy(analysis = value) }
                }
                EditorField("错误答案", draft.wrongAnswerText) {
                    val value = it
                    update { current -> current.copy(wrongAnswerText = value) }
                }
                Text("错误状态")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    MistakeStatus.entries.forEach { status ->
                        BanduFilterChip(
                            label = status.editorLabel(),
                            selected = draft.mistakeStatus == status,
                            onClick = { update { it.copy(mistakeStatus = status) } },
                        )
                    }
                }
                EditorField("错误分析", draft.mistakeAnalysis) {
                    val value = it
                    update { current -> current.copy(mistakeAnalysis = value) }
                }
                EditorField("学科", draft.subject) {
                    val value = it
                    update { current -> current.copy(subject = value) }
                }
                Text("题集")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    state.availableCollections.forEach { collection ->
                        BanduFilterChip(
                            label = collection.name,
                            selected = draft.collectionId == collection.id,
                            onClick = {
                                update { it.copy(collectionId = collection.id) }
                            },
                        )
                    }
                }
                Text("标签（最多 5 个）")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    state.availableTags.forEach { tag ->
                        BanduFilterChip(
                            label = tag.name,
                            selected = tag.id in draft.tagIds,
                            onClick = {
                                val next = toggleEditorTag(draft.tagIds, tag.id)
                                update { it.copy(tagIds = next) }
                            },
                        )
                    }
                }
                EditorField("年级/学期", draft.gradeSemester) {
                    val value = it
                    update { current -> current.copy(gradeSemester = value) }
                }
                Text("试卷等级")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    (listOf<PaperLevel?>(null) + PaperLevel.entries).forEach { level ->
                        BanduFilterChip(
                            label = level.editorLabel(),
                            selected = draft.paperLevel == level,
                            onClick = { update { it.copy(paperLevel = level) } },
                        )
                    }
                }
                EditorField("笔记", draft.notes) {
                    val value = it
                    update { current -> current.copy(notes = value) }
                }
                state.editorErrorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(ErrorItemDetailAction.SaveEditor) },
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "保存中" else "保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(ErrorItemDetailAction.DismissEditor) },
                enabled = !state.isSaving,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
    )
}

private fun MistakeStatus.editorLabel(): String = when (this) {
    MistakeStatus.WRONG_ATTEMPT -> "有错误作答"
    MistakeStatus.NOT_ATTEMPTED -> "未作答"
    MistakeStatus.UNKNOWN -> "无法判断"
}

private fun PaperLevel?.editorLabel(): String = when (this) {
    PaperLevel.A -> "A 卷"
    PaperLevel.B -> "B 卷"
    PaperLevel.OTHER -> "其他"
    null -> "未设置"
}

internal fun toggleEditorTag(
    selected: Set<TagId>,
    tagId: TagId,
): Set<TagId> =
    when {
        tagId in selected -> selected - tagId
        selected.size < ErrorItemDraft.MAX_TAG_COUNT -> selected + tagId
        else -> selected
    }
