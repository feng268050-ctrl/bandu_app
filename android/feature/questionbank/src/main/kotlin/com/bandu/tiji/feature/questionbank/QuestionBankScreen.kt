package com.bandu.tiji.feature.questionbank

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.bandu.tiji.core.designsystem.component.BanduBackNavigation
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
import com.bandu.tiji.core.designsystem.component.BanduEmptyState
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.questionbank.BankQuestion
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamAttempt
import com.bandu.tiji.core.model.questionbank.ExamGradingResult
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.ExamSessionStatus
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankImportStatus
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuestionBankContent(
    uiState: QuestionBankUiState,
    onAction: (QuestionBankAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState.screen) {
        QuestionBankScreen.List -> "PDF 题库"
        is QuestionBankScreen.Detail -> "题库详情"
        is QuestionBankScreen.Exam -> "随机考卷"
    }
    BanduPageScaffold(
        title = title,
        modifier = modifier,
        navigation = {
            BanduBackNavigation(onBack = { onAction(QuestionBankAction.NavigateBack) })
        },
        actions = {
            when (uiState.screen) {
                QuestionBankScreen.List -> {
                    TextButton(onClick = { onAction(QuestionBankAction.RequestPdfImport) }) {
                        Text("导入 PDF")
                    }
                }
                is QuestionBankScreen.Detail -> {
                    TextButton(onClick = { onAction(QuestionBankAction.OpenQuestionEditor) }) {
                        Text("添加题目")
                    }
                }
                is QuestionBankScreen.Exam -> Unit
            }
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = uiState.loadingMessage,
                modifier = Modifier.padding(contentPadding),
            )
            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(QuestionBankAction.Retry) },
                modifier = Modifier.padding(contentPadding),
            )
            else -> when (uiState.screen) {
                QuestionBankScreen.List -> BankListContent(
                    banks = uiState.banks,
                    notice = uiState.noticeMessage,
                    onAction = onAction,
                    modifier = Modifier.padding(contentPadding),
                )
                is QuestionBankScreen.Detail -> BankDetailContent(
                    bank = uiState.currentBank,
                    examSessions = uiState.examSessions,
                    notice = uiState.noticeMessage,
                    onAction = onAction,
                    modifier = Modifier.padding(contentPadding),
                )
                is QuestionBankScreen.Exam -> ExamContent(
                    session = uiState.currentExam,
                    currentAttemptIndex = uiState.currentAttemptIndex,
                    answerInput = uiState.answerInput,
                    notice = uiState.noticeMessage,
                    onAction = onAction,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }

    uiState.questionEditor?.let { editor ->
        QuestionEditorDialog(
            editor = editor,
            onAction = onAction,
        )
    }
    uiState.examDialog?.let { dialog ->
        GenerateExamDialog(
            dialog = dialog,
            onAction = onAction,
        )
    }
    uiState.pendingDeleteBank?.let { pending ->
        DeleteQuestionBankDialog(
            pending = pending,
            onAction = onAction,
        )
    }
    uiState.renameExamDialog?.let { dialog ->
        RenameExamDialog(
            dialog = dialog,
            onAction = onAction,
        )
    }
    uiState.pendingDeleteExam?.let { pending ->
        DeleteExamDialog(
            pending = pending,
            onAction = onAction,
        )
    }
}

@Composable
private fun BankListContent(
    banks: List<QuestionBankSummary>,
    notice: String?,
    onAction: (QuestionBankAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banks.isEmpty()) {
        BanduEmptyState(
            title = "还没有 PDF 题库",
            description = "导入 PDF 后自动拆题，复核后可随机生成考卷",
            actionLabel = "导入 PDF",
            onAction = { onAction(QuestionBankAction.RequestPdfImport) },
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(BanduSpacing.PageHorizontal)
            .testTag("question-bank-list"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        notice?.let {
            item { NoticeCard(it) }
        }
        items(banks, key = { it.id.value }) { bank ->
            BanduCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question-bank-${bank.id.value}")
                    .combinedClickable(
                        role = Role.Button,
                        onClick = { onAction(QuestionBankAction.OpenBank(bank.id)) },
                        onLongClick = { onAction(QuestionBankAction.RequestDeleteBank(bank.id)) },
                    ),
            ) {
                Text(bank.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${bank.questionCount} 道题 · ${bank.importStatus.label()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "来源：${bank.sourceFileName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "最近更新 ${formatEpoch(bank.updatedAtEpochMillis)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BankDetailContent(
    bank: QuestionBank?,
    examSessions: List<ExamSessionSummary>,
    notice: String?,
    onAction: (QuestionBankAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bank == null) {
        BanduEmptyState(
            title = "题库不存在",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(BanduSpacing.PageHorizontal)
            .testTag("question-bank-detail"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        notice?.let {
            item { NoticeCard(it) }
        }
        item {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text(bank.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "来源：${bank.sourceFileName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${bank.questions.size} 道题 · ${bank.importStatus.label()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "PDF 已自动拆题入库；请复核题目、答案和解析后再生成考卷。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    Button(
                        onClick = { onAction(QuestionBankAction.OpenGenerateExam) },
                        enabled = bank.questions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("生成考卷")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
                    ) {
                        OutlinedButton(
                            onClick = { onAction(QuestionBankAction.RequestDeleteBank(bank.id)) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("删除题库")
                        }
                        OutlinedButton(
                            onClick = { onAction(QuestionBankAction.OpenQuestionEditor) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("添加题目")
                        }
                    }
                }
            }
        }
        item {
            ExamHistoryCard(
                sessions = examSessions,
                onAction = onAction,
            )
        }
        if (bank.questions.isEmpty()) {
            item {
                BanduEmptyState(
                    title = "题库还没有题目",
                    description = "先手动添加题目、答案和解析，再随机生成考卷",
                    actionLabel = "添加题目",
                    onAction = { onAction(QuestionBankAction.OpenQuestionEditor) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(bank.questions, key = { it.id.value }) { question ->
                QuestionCard(question)
            }
        }
    }
}

@Composable
private fun ExamHistoryCard(
    sessions: List<ExamSessionSummary>,
    onAction: (QuestionBankAction) -> Unit,
) {
    var isManaging by remember { mutableStateOf(false) }
    val canManage = sessions.isNotEmpty()
    val showManageActions = isManaging && canManage
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
        ) {
            Text(
                "考卷历史",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { isManaging = !isManaging },
                enabled = canManage,
            ) {
                Text(if (showManageActions) "完成" else "管理考卷")
            }
        }
        if (sessions.isEmpty()) {
            Text(
                "暂无历史考卷",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                sessions.forEach { session ->
                    Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
                        OutlinedButton(
                            onClick = { onAction(QuestionBankAction.OpenExam(session.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(session.title, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "${session.questionCount} 题 · ${session.status.label()} · ${formatEpoch(session.createdAtEpochMillis)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (showManageActions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
                            ) {
                                TextButton(
                                    onClick = { onAction(QuestionBankAction.RequestRenameExam(session.id)) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("重命名")
                                }
                                TextButton(
                                    onClick = { onAction(QuestionBankAction.RequestDeleteExam(session.id)) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("删除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(question: BankQuestion) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(question.stem, style = MaterialTheme.typography.titleMedium)
        if (question.options.isNotEmpty()) {
            Text(
                question.options.joinToString("\n"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "类型：${question.questionType.label()} · 难度：${question.difficulty.label()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "答案：${question.answer?.ifBlank { null } ?: "暂无"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ExamContent(
    session: ExamSession?,
    currentAttemptIndex: Int,
    answerInput: String,
    notice: String?,
    onAction: (QuestionBankAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (session == null || session.attempts.isEmpty()) {
        BanduEmptyState(
            title = "考卷不存在",
            modifier = modifier,
        )
        return
    }
    val attempt = session.attempts[currentAttemptIndex.coerceIn(session.attempts.indices)]
    val isLast = currentAttemptIndex == session.attempts.lastIndex
    val answeredCount = session.attempts.count { it.answerRevealed }
    val allAnswered = answeredCount == session.attempts.size
    val scoreText = session.scoreText()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(BanduSpacing.PageHorizontal)
            .testTag("exam-session"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        notice?.let {
            item { NoticeCard(it) }
        }
        item {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text(session.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "第 ${currentAttemptIndex + 1} / ${session.attempts.size} 题 · 已完成 $answeredCount/${session.attempts.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (session.status == ExamSessionStatus.COMPLETED || allAnswered) {
                    Text(
                        scoreText,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (session.status != ExamSessionStatus.COMPLETED && allAnswered) {
                    Button(onClick = { onAction(QuestionBankAction.SubmitExam) }) {
                        Text("提交整卷")
                    }
                }
            }
        }
        item {
            ExamAttemptCard(
                attempt = attempt,
                questionNumber = currentAttemptIndex + 1,
                answerInput = answerInput,
                onAction = onAction,
            )
        }
        if (attempt.answerRevealed) {
            item {
                ExamAnswerCard(
                    attempt = attempt,
                    isLast = isLast,
                    allAnswered = allAnswered,
                    sessionCompleted = session.status == ExamSessionStatus.COMPLETED,
                    scoreText = scoreText,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ExamAttemptCard(
    attempt: ExamAttempt,
    questionNumber: Int,
    answerInput: String,
    onAction: (QuestionBankAction) -> Unit,
) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$questionNumber. ${attempt.question.stem.withoutLeadingQuestionNumber()}",
            style = MaterialTheme.typography.titleMedium,
        )
        if (!attempt.answerRevealed) {
            AnswerInput(
                attempt = attempt,
                answerInput = answerInput,
                onAction = onAction,
            )
            Button(onClick = { onAction(QuestionBankAction.SubmitCurrentAnswer) }) {
                Text("提交本题")
            }
        } else {
            SubmittedQuestionOptions(attempt)
        }
    }
}

@Composable
private fun SubmittedQuestionOptions(attempt: ExamAttempt) {
    if (attempt.question.options.isEmpty()) {
        Text(
            "本题已提交，答案和解析见下方。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
        attempt.question.options.forEach { option ->
            Text(option, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExamAnswerCard(
    attempt: ExamAttempt,
    isLast: Boolean,
    allAnswered: Boolean,
    sessionCompleted: Boolean,
    scoreText: String,
    onAction: (QuestionBankAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(BanduSpacing.PageHorizontal),
                horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "答案 ${attempt.question.answer?.ifBlank { null } ?: "暂无"}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "你选择 ${attempt.userAnswer.orEmpty()}",
                    color = resultColor(attempt.gradingResult),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        attempt.question.analysis?.takeIf { it.isNotBlank() }?.let {
            Text("解析：$it", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "批改：${attempt.gradingResult.label()}",
            color = resultColor(attempt.gradingResult),
            style = MaterialTheme.typography.titleSmall,
        )
        attempt.gradingFeedback?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (isLast && (allAnswered || sessionCompleted)) {
            Text(
                scoreText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (!isLast) {
            Button(onClick = { onAction(QuestionBankAction.NextAttempt) }) {
                Text("下一题")
            }
        } else if (!sessionCompleted && allAnswered) {
            Button(onClick = { onAction(QuestionBankAction.SubmitExam) }) {
                Text("提交整卷")
            }
        } else if (!sessionCompleted && !allAnswered) {
            Text(
                "已到最后一题",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AnswerInput(
    attempt: ExamAttempt,
    answerInput: String,
    onAction: (QuestionBankAction) -> Unit,
) {
    if (attempt.question.options.isEmpty()) {
        OutlinedTextField(
            value = answerInput,
            onValueChange = { onAction(QuestionBankAction.UpdateAnswer(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("你的答案") },
        )
        return
    }
    val multiChoice = attempt.question.questionType == BankQuestionType.MULTIPLE_CHOICE
    val optionValues = attempt.question.options.mapIndexed { index, option ->
        optionAnswerValue(option, index)
    }
    val selectedValues = answerInput.toAnswerSet()
    Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
        attempt.question.options.forEachIndexed { index, option ->
            val value = optionValues[index]
            val selected = selectedValues.contains(value)
            AnswerOptionButton(
                text = option,
                selected = selected,
                onClick = {
                    val nextAnswer = if (multiChoice) {
                        val next = if (selected) {
                            selectedValues - value
                        } else {
                            selectedValues + value
                        }
                        optionValues.filter { next.contains(it) }.joinToString(",")
                    } else {
                        value
                    }
                    onAction(QuestionBankAction.UpdateAnswer(nextAnswer))
                },
            )
        }
    }
}

@Composable
private fun AnswerOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(text)
        }
    }
}

@Composable
private fun QuestionEditorDialog(
    editor: QuestionEditorUiState,
    onAction: (QuestionBankAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(QuestionBankAction.DismissQuestionEditor) },
        title = { Text("添加题目") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
            ) {
                OutlinedTextField(
                    value = editor.stem,
                    onValueChange = { onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(stem = it))) },
                    label = { Text("题干") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = editor.optionsText,
                    onValueChange = { onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(optionsText = it))) },
                    label = { Text("选项（每行一个，可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = editor.answer,
                    onValueChange = { onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(answer = it))) },
                    label = { Text("答案") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = editor.analysis,
                    onValueChange = { onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(analysis = it))) },
                    label = { Text("解析") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Text("题型", style = MaterialTheme.typography.labelLarge)
                EnumChoiceRows(
                    values = BankQuestionType.entries,
                    selected = editor.questionType,
                    label = BankQuestionType::label,
                    onSelect = {
                        onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(questionType = it)))
                    },
                )
                Text("难度", style = MaterialTheme.typography.labelLarge)
                EnumChoiceRows(
                    values = ExerciseDifficulty.entries,
                    selected = editor.difficulty,
                    label = ExerciseDifficulty::label,
                    onSelect = {
                        onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(difficulty = it)))
                    },
                )
                OutlinedTextField(
                    value = editor.tagsText,
                    onValueChange = { onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(tagsText = it))) },
                    label = { Text("标签（逗号或换行分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.sourcePageText,
                    onValueChange = {
                        onAction(QuestionBankAction.UpdateQuestionEditor(editor.copy(sourcePageText = it)))
                    },
                    label = { Text("来源页码（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                editor.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(QuestionBankAction.SaveQuestion) },
                enabled = !editor.isSaving,
            ) {
                Text(if (editor.isSaving) "保存中" else "保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(QuestionBankAction.DismissQuestionEditor) },
                enabled = !editor.isSaving,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun RenameExamDialog(
    dialog: RenameExamUiState,
    onAction: (QuestionBankAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!dialog.isSaving) onAction(QuestionBankAction.DismissRenameExam)
        },
        title = { Text("重命名考卷") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                OutlinedTextField(
                    value = dialog.titleText,
                    onValueChange = {
                        onAction(
                            QuestionBankAction.UpdateRenameExam(
                                dialog.copy(titleText = it, errorMessage = null),
                            ),
                        )
                    },
                    label = { Text("考卷名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = dialog.errorMessage != null,
                )
                dialog.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(QuestionBankAction.ConfirmRenameExam) },
                enabled = !dialog.isSaving,
            ) {
                Text(if (dialog.isSaving) "保存中" else "保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(QuestionBankAction.DismissRenameExam) },
                enabled = !dialog.isSaving,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun DeleteExamDialog(
    pending: DeleteExamUiState,
    onAction: (QuestionBankAction) -> Unit,
) {
    BanduDangerConfirmationDialog(
        title = "删除考卷",
        message = listOf(
            "将删除考卷“${pending.title}”。",
            "本次作答记录会一并删除，此操作不可撤销。",
            pending.errorMessage,
        ).filterNotNull().joinToString("\n"),
        confirmLabel = if (pending.isDeleting) "删除中" else "确认删除",
        onConfirm = { onAction(QuestionBankAction.ConfirmDeleteExam) },
        onDismiss = { onAction(QuestionBankAction.DismissDeleteExam) },
    )
}

@Composable
private fun DeleteQuestionBankDialog(
    pending: DeleteQuestionBankUiState,
    onAction: (QuestionBankAction) -> Unit,
) {
    BanduDangerConfirmationDialog(
        title = "删除题库",
        message = listOf(
            "将删除题库“${pending.bankName}”。",
            "来源：${pending.sourceFileName}",
            "题目和历史考卷会一并删除，此操作不可撤销。",
            pending.errorMessage,
        ).filterNotNull().joinToString("\n"),
        confirmLabel = if (pending.isDeleting) "删除中" else "确认删除",
        onConfirm = { onAction(QuestionBankAction.ConfirmDeleteBank) },
        onDismiss = { onAction(QuestionBankAction.DismissDeleteBank) },
    )
}

@Composable
private fun GenerateExamDialog(
    dialog: GenerateExamUiState,
    onAction: (QuestionBankAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(QuestionBankAction.DismissGenerateExam) },
        title = { Text("生成考卷") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                OutlinedTextField(
                    value = dialog.questionCountText,
                    onValueChange = {
                        onAction(QuestionBankAction.UpdateGenerateExam(dialog.copy(questionCountText = it)))
                    },
                    label = { Text("抽题数量") },
                    singleLine = true,
                )
                dialog.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(QuestionBankAction.CreateExam) },
                enabled = !dialog.isCreating,
            ) {
                Text(if (dialog.isCreating) "生成中" else "生成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(QuestionBankAction.DismissGenerateExam) },
                enabled = !dialog.isCreating,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun <T> EnumChoiceRows(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
        values.chunked(2).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                rowValues.forEach { value ->
                    if (value == selected) {
                        Button(onClick = { onSelect(value) }) {
                            Text(label(value))
                        }
                    } else {
                        OutlinedButton(onClick = { onSelect(value) }) {
                            Text(label(value))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(message: String) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun resultColor(result: ExamGradingResult) =
    when (result) {
        ExamGradingResult.CORRECT -> MaterialTheme.colorScheme.primary
        ExamGradingResult.INCORRECT -> MaterialTheme.colorScheme.error
        ExamGradingResult.NEEDS_REVIEW,
        ExamGradingResult.NOT_GRADED,
        -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun QuestionBankImportStatus.label(): String =
    when (this) {
        QuestionBankImportStatus.PENDING -> "等待导入"
        QuestionBankImportStatus.REVIEW_REQUIRED -> "需要复核"
        QuestionBankImportStatus.IMPORTED -> "已导入"
        QuestionBankImportStatus.FAILED -> "导入失败"
    }

private fun BankQuestionType.label(): String =
    when (this) {
        BankQuestionType.SINGLE_CHOICE -> "单选"
        BankQuestionType.MULTIPLE_CHOICE -> "多选"
        BankQuestionType.FILL_BLANK -> "填空"
        BankQuestionType.SUBJECTIVE -> "主观"
        BankQuestionType.UNKNOWN -> "未知"
    }

private fun ExerciseDifficulty.label(): String =
    when (this) {
        ExerciseDifficulty.EASY -> "简单"
        ExerciseDifficulty.MEDIUM -> "中等"
        ExerciseDifficulty.HARD -> "困难"
        ExerciseDifficulty.CHALLENGE -> "挑战"
    }

private fun ExamSessionStatus.label(): String =
    when (this) {
        ExamSessionStatus.IN_PROGRESS -> "进行中"
        ExamSessionStatus.COMPLETED -> "已完成"
    }

private fun ExamGradingResult.label(): String =
    when (this) {
        ExamGradingResult.CORRECT -> "正确"
        ExamGradingResult.INCORRECT -> "错误"
        ExamGradingResult.NEEDS_REVIEW -> "待复核"
        ExamGradingResult.NOT_GRADED -> "未批改"
    }

private fun ExamSession.scoreText(): String {
    val correct = attempts.count { it.gradingResult == ExamGradingResult.CORRECT }
    val total = attempts.size.coerceAtLeast(1)
    val percent = (correct * 100) / total
    return "得分：$correct/${attempts.size}（$percent 分）"
}

private fun String.withoutLeadingQuestionNumber(): String {
    val trimmed = trim()
    val withoutDelimitedNumber = trimmed.replaceFirst(
        Regex("""^(?:第\s*)?(?:\d{1,4}|[一二三四五六七八九十百千]+)\s*(?:题)?[、.．:：)]\s*"""),
        "",
    )
    val withoutQuestionNumber = withoutDelimitedNumber.replaceFirst(
        Regex("""^第\s*(?:\d{1,4}|[一二三四五六七八九十百千]+)\s*题\s*"""),
        "",
    )
    return withoutQuestionNumber.ifBlank { trimmed }
}

private fun optionAnswerValue(option: String, index: Int): String {
    val marker = Regex("""^\s*([A-Za-z])[\s.．、):：]""")
        .find(option)
        ?.groupValues
        ?.getOrNull(1)
        ?.uppercase()
    return marker ?: ('A' + index).toString()
}

private fun String.toAnswerSet(): Set<String> =
    uppercase()
        .split(Regex("[,，、\\s]+"))
        .flatMap { token ->
            val trimmed = token.trim()
            when {
                trimmed.matches(Regex("[A-Z]{2,}")) -> trimmed.map { it.toString() }
                else -> listOf(trimmed)
            }
        }
        .filter { it.isNotBlank() }
        .toSet()

private fun formatEpoch(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
