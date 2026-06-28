package com.bandu.tiji.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.PaperLevel

@Composable
fun CaptureReviewScreen(
    uiState: CaptureUiState,
    onAction: (CaptureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = uiState.reviewDraft ?: return
    fun update(transform: (CaptureReviewDraft) -> CaptureReviewDraft) {
        onAction(CaptureAction.UpdateReviewDraft(transform(draft)))
    }

    BanduPageScaffold(
        title = "确认错题",
        modifier = modifier,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal)
                .verticalScroll(rememberScrollState())
                .testTag(REVIEW_SCREEN_TAG),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            uiState.qualityWarning?.let { Text(it) }
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("题集")
                uiState.availableCollections.forEach { collection ->
                    FilterChip(
                        selected = draft.collectionId == collection.id,
                        onClick = { onAction(CaptureAction.SelectReviewCollection(collection.id)) },
                        label = { Text(collection.name) },
                    )
                }
            }
            CaptureTextField(
                tag = REVIEW_SUBJECT_TAG,
                label = "学科",
                value = draft.subject,
                onValueChange = { value ->
                    update { current -> current.copy(subject = value) }
                },
            )
            CaptureTextField(REVIEW_QUESTION_TAG, "题目", draft.questionText) {
                update { current -> current.copy(questionText = it) }
            }
            CaptureTextField(REVIEW_ANSWER_TAG, "答案", draft.answerText) {
                update { current -> current.copy(answerText = it) }
            }
            CaptureTextField(REVIEW_ANALYSIS_TAG, "解析", draft.analysis) {
                update { current -> current.copy(analysis = it) }
            }
            CaptureTextField(REVIEW_WRONG_ANSWER_TAG, "我的错误答案", draft.wrongAnswerText) {
                update { current -> current.copy(wrongAnswerText = it) }
            }
            CaptureTextField(REVIEW_MISTAKE_ANALYSIS_TAG, "错误原因", draft.mistakeAnalysis) {
                update { current -> current.copy(mistakeAnalysis = it) }
            }
            CaptureTextField(REVIEW_GRADE_TAG, "年级学期", draft.gradeSemester.orEmpty()) {
                update { current -> current.copy(gradeSemester = it.ifBlank { null }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                PaperLevel.entries.forEach { level ->
                    FilterChip(
                        selected = draft.paperLevel == level,
                        onClick = { update { it.copy(paperLevel = level) } },
                        label = { Text("试卷等级 $level") },
                    )
                }
            }
            CaptureTextField(REVIEW_NOTES_TAG, "笔记", draft.notes) {
                update { current -> current.copy(notes = it) }
            }
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text("标签（最多 5 个）")
                Row(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    uiState.availableTags.forEach { tag ->
                        AssistChip(
                            enabled = tag.id in draft.tagIds || draft.tagIds.size < 5,
                            onClick = { onAction(CaptureAction.ToggleReviewTag(tag.id)) },
                            label = { Text(tag.name) },
                            modifier = Modifier.testTag("$REVIEW_TAG_PREFIX${tag.id.value}"),
                        )
                    }
                }
                Text("已选择 ${draft.selectedTagCount}/5")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                OutlinedButton(
                    onClick = { onAction(CaptureAction.Cancel) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onAction(CaptureAction.SaveReview) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun CaptureTextField(
    tag: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        label = { Text(label) },
        minLines = 1,
    )
}

internal const val REVIEW_SCREEN_TAG = "capture-review-screen"
internal const val REVIEW_SUBJECT_TAG = "capture-review-subject"
internal const val REVIEW_QUESTION_TAG = "capture-review-question"
internal const val REVIEW_ANSWER_TAG = "capture-review-answer"
internal const val REVIEW_ANALYSIS_TAG = "capture-review-analysis"
internal const val REVIEW_WRONG_ANSWER_TAG = "capture-review-wrong-answer"
internal const val REVIEW_MISTAKE_ANALYSIS_TAG = "capture-review-mistake-analysis"
internal const val REVIEW_GRADE_TAG = "capture-review-grade"
internal const val REVIEW_NOTES_TAG = "capture-review-notes"
internal const val REVIEW_TAG_PREFIX = "capture-review-tag-"
