package com.bandu.tiji.feature.questionbank

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.bandu.tiji.core.designsystem.component.BanduBackNavigation
import com.bandu.tiji.core.designsystem.component.BanduCard
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
                message = "正在加载题库",
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
                    notice = uiState.noticeMessage,
                    onAction = onAction,
                    modifier = Modifier.padding(contentPadding),
                )
                is QuestionBankScreen.Exam -> ExamContent(
                    session = uiState.currentExam,
                    currentAttemptIndex = uiState.currentAttemptIndex,
                    answerInput = uiState.answerInput,
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
            description = "导入 PDF 后可手动复核题目，并随机生成考卷",
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
                    .clickable(
                        role = Role.Button,
                        onClick = { onAction(QuestionBankAction.OpenBank(bank.id)) },
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
                    "当前版本支持 PDF 文件建库和人工复核录题；自动 OCR/AI 拆题将在后续阶段接入。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small)) {
                    Button(
                        onClick = { onAction(QuestionBankAction.OpenGenerateExam) },
                        enabled = bank.questions.isNotEmpty(),
                    ) {
                        Text("生成考卷")
                    }
                    OutlinedButton(onClick = { onAction(QuestionBankAction.OpenQuestionEditor) }) {
                        Text("添加题目")
                    }
                }
            }
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(BanduSpacing.PageHorizontal)
            .testTag("exam-session"),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
    ) {
        item {
            BanduCard(modifier = Modifier.fillMaxWidth()) {
                Text(session.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "第 ${currentAttemptIndex + 1} / ${session.attempts.size} 题 · ${session.status.label()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            ExamAttemptCard(
                attempt = attempt,
                answerInput = answerInput,
                isLast = isLast,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ExamAttemptCard(
    attempt: ExamAttempt,
    answerInput: String,
    isLast: Boolean,
    onAction: (QuestionBankAction) -> Unit,
) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(attempt.question.stem, style = MaterialTheme.typography.titleMedium)
        if (attempt.question.options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(BanduSpacing.ExtraSmall)) {
                attempt.question.options.forEach { option ->
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (!attempt.answerRevealed) {
            OutlinedTextField(
                value = answerInput,
                onValueChange = { onAction(QuestionBankAction.UpdateAnswer(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("你的答案") },
            )
            Button(onClick = { onAction(QuestionBankAction.SubmitCurrentAnswer) }) {
                Text("提交并查看答案")
            }
        } else {
            Text(
                "你的答案：${attempt.userAnswer.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "标准答案：${attempt.question.answer?.ifBlank { null } ?: "暂无标准答案"}",
                style = MaterialTheme.typography.bodyMedium,
            )
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
            if (!isLast) {
                Button(onClick = { onAction(QuestionBankAction.NextAttempt) }) {
                    Text("下一题")
                }
            } else {
                Text(
                    "已到最后一题",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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

private fun formatEpoch(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
