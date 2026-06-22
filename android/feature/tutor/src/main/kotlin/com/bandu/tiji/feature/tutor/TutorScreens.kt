package com.bandu.tiji.feature.tutor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.markdown.MarkdownLatexView
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(BanduSpacing.PageHorizontal),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        when {
            uiState.isLoading -> item {
                Text("正在加载会话")
            }
            uiState.sessions.isEmpty() -> item {
                Text("暂无辅导会话")
            }
            else -> items(
                items = uiState.sessions,
                key = { it.id.value },
            ) { session ->
                TutorSessionSummaryCard(
                    session = session,
                    onClick = {
                        onAction(TutorSessionsAction.OpenSession(session.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun TutorSessionSummaryCard(
    session: TutorSessionSummary,
    onClick: () -> Unit,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = session.title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (session.errorItemId == null) {
                "未绑定错题"
            } else {
                "已绑定错题"
            },
        )
        Text(
            text = "更新于 ${formatTutorTimestamp(session.updatedAtEpochMillis)}",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun formatTutorTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TUTOR_TIME_FORMATTER)

private val TUTOR_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
fun TutorSessionScreen(
    uiState: TutorSessionUiState,
    onAction: (TutorSessionAction) -> Unit,
    modifier: Modifier = Modifier,
    markdownRenderer: @Composable (String, Modifier, Boolean) -> Unit =
        { markdown, contentModifier, streaming ->
            MarkdownLatexView(
                markdown = markdown,
                modifier = contentModifier,
                streaming = streaming,
            )
        },
) {
    BanduPageScaffold(
        title = uiState.session?.title ?: "辅导会话",
        modifier = modifier,
    ) { padding ->
        TutorSessionContent(
            uiState = uiState,
            padding = padding,
            markdownRenderer = markdownRenderer,
            onAction = onAction,
        )
    }
}

@Composable
private fun TutorSessionContent(
    uiState: TutorSessionUiState,
    padding: PaddingValues,
    markdownRenderer: @Composable (String, Modifier, Boolean) -> Unit,
    onAction: (TutorSessionAction) -> Unit,
) {
    val session = uiState.session
    if (uiState.isLoading || session == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(BanduSpacing.PageHorizontal),
        ) {
            Text(
                uiState.errorMessage ?: if (uiState.isLoading) {
                    "正在加载会话"
                } else {
                    "会话不存在"
                },
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("tutor-session-messages"),
            contentPadding = PaddingValues(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            items(
                items = session.messages,
                key = { it.id.value },
            ) { message ->
                TutorMessageCard(
                    message = message,
                    markdownRenderer = markdownRenderer,
                )
            }
            if (uiState.streamingText.isNotEmpty()) {
                item(key = "streaming-message") {
                    BanduCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tutor-streaming-message"),
                    ) {
                        Text("AI辅导")
                        markdownRenderer(
                            uiState.streamingText,
                            Modifier.fillMaxWidth(),
                            true,
                        )
                    }
                }
            }
            items(
                items = session.exercises,
                key = { "exercise-${it.id.value}" },
            ) { exercise ->
                val grade = uiState.exerciseGrades[exercise.id]
                BanduCard(modifier = Modifier.fillMaxWidth()) {
                    Text("类似练习 · ${exercise.difficulty.tutorLabel()}")
                    markdownRenderer(exercise.questionText, Modifier.fillMaxWidth(), false)
                    OutlinedTextField(
                        value = uiState.exerciseAnswers[exercise.id].orEmpty(),
                        onValueChange = {
                            onAction(
                                TutorSessionAction.UpdateExerciseAnswer(exercise.id, it),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("你的答案") },
                    )
                    Button(
                        onClick = {
                            onAction(TutorSessionAction.GradeExercise(exercise.id))
                        },
                        enabled = exercise.id !in uiState.gradingExerciseIds,
                    ) {
                        Text(
                            if (exercise.id in uiState.gradingExerciseIds) {
                                "批改中"
                            } else {
                                "提交批改"
                            },
                        )
                    }
                    grade?.let {
                        Text("批改结果：${it.finalResult.gradeLabel()}")
                        Text(it.feedback)
                    }
                }
            }
        }
        uiState.errorMessage?.let { Text(it) }
        Text(
            "生成类似练习",
            modifier = Modifier.padding(horizontal = BanduSpacing.PageHorizontal),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = BanduSpacing.PageHorizontal),
            horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
        ) {
            ExerciseDifficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = uiState.selectedDifficulty == difficulty,
                    onClick = {
                        onAction(TutorSessionAction.SelectDifficulty(difficulty))
                    },
                    label = { Text(difficulty.tutorLabel()) },
                )
            }
        }
        Button(
            onClick = { onAction(TutorSessionAction.GenerateExercise) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BanduSpacing.PageHorizontal),
            enabled = uiState.selectedDifficulty != null &&
                !uiState.isGeneratingExercise &&
                !uiState.isStreaming,
        ) {
            Text(if (uiState.isGeneratingExercise) "生成中" else "生成练习")
        }
        uiState.exerciseErrorMessage?.let { Text(it) }
        OutlinedTextField(
            value = uiState.input,
            onValueChange = { onAction(TutorSessionAction.UpdateInput(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BanduSpacing.PageHorizontal),
            label = { Text("输入问题") },
            enabled = !uiState.isStreaming,
        )
        Button(
            onClick = { onAction(TutorSessionAction.Send) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BanduSpacing.PageHorizontal),
            enabled = uiState.input.isNotBlank() && !uiState.isStreaming,
        ) {
            Text(if (uiState.isStreaming) "生成中" else "发送")
        }
        if (!uiState.isStreaming) {
            Button(
                onClick = { onAction(TutorSessionAction.RequestStepByStep) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BanduSpacing.PageHorizontal),
            ) {
                Text("分步讲解")
            }
        }
        if (uiState.isStreaming) {
            Button(
                onClick = { onAction(TutorSessionAction.StopGeneration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BanduSpacing.PageHorizontal),
            ) {
                Text("停止生成")
            }
        }
    }
}

private fun ExerciseDifficulty.tutorLabel(): String = when (this) {
    ExerciseDifficulty.EASY -> "简单"
    ExerciseDifficulty.MEDIUM -> "普通"
    ExerciseDifficulty.HARD -> "困难"
    ExerciseDifficulty.CHALLENGE -> "挑战"
}

private fun GradeResult.gradeLabel(): String = when (this) {
    GradeResult.CORRECT -> "正确"
    GradeResult.INCORRECT -> "错误"
    GradeResult.NEEDS_REVIEW -> "待复核"
}

@Composable
private fun TutorMessageCard(
    message: TutorMessage,
    markdownRenderer: @Composable (String, Modifier, Boolean) -> Unit,
) {
    BanduCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tutor-message-${message.id.value}"),
    ) {
        Text(
            when (message.role) {
                TutorMessageRole.USER -> "我"
                TutorMessageRole.ASSISTANT -> "AI辅导"
                TutorMessageRole.SYSTEM_LOCAL -> "系统"
            },
        )
        markdownRenderer(
            message.content,
            Modifier.fillMaxWidth(),
            false,
        )
    }
}
