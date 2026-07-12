package com.bandu.tiji.data.mapper

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId
import com.bandu.tiji.core.model.questionbank.BankQuestion
import com.bandu.tiji.core.model.questionbank.BankQuestionDraft
import com.bandu.tiji.core.model.questionbank.BankQuestionType
import com.bandu.tiji.core.model.questionbank.ExamAttempt
import com.bandu.tiji.core.model.questionbank.ExamGradingResult
import com.bandu.tiji.core.model.questionbank.ExamGradingSource
import com.bandu.tiji.core.model.questionbank.ExamSession
import com.bandu.tiji.core.model.questionbank.ExamSessionStatus
import com.bandu.tiji.core.model.questionbank.ExamSessionSummary
import com.bandu.tiji.core.model.questionbank.QuestionBank
import com.bandu.tiji.core.model.questionbank.QuestionBankDraft
import com.bandu.tiji.core.model.questionbank.QuestionBankImportStatus
import com.bandu.tiji.core.model.questionbank.QuestionBankSummary
import com.bandu.tiji.core.model.questionbank.QuestionReviewStatus
import com.bandu.tiji.core.storage.db.entity.BankQuestionEntity
import com.bandu.tiji.core.storage.db.entity.ExamAttemptEntity
import com.bandu.tiji.core.storage.db.entity.ExamSessionEntity
import com.bandu.tiji.core.storage.db.entity.QuestionBankEntity
import com.bandu.tiji.core.storage.db.projection.ExamAttemptQuestionProjection
import com.bandu.tiji.core.storage.db.projection.ExamSessionSummaryProjection
import com.bandu.tiji.core.storage.db.projection.QuestionBankSummaryProjection

private const val TEXT_SEPARATOR = "\u001F"

internal fun QuestionBankDraft.toEntity(
    id: QuestionBankId,
    now: Long,
): QuestionBankEntity =
    QuestionBankEntity(
        id = id.value,
        name = name.trim(),
        sourceType = "PDF",
        sourceFileName = sourceFileName.trim(),
        sourceUri = sourceUri,
        subject = subject?.trim()?.ifEmpty { null },
        importStatus = QuestionBankImportStatus.REVIEW_REQUIRED.name,
        createdAt = now,
        updatedAt = now,
    )

internal fun QuestionBankSummaryProjection.toDomain(): QuestionBankSummary =
    QuestionBankSummary(
        id = QuestionBankId(id),
        name = name,
        sourceFileName = sourceFileName,
        sourceUri = sourceUri,
        subject = subject,
        questionCount = questionCount,
        importStatus = enumValueOrDefault(importStatus, QuestionBankImportStatus.REVIEW_REQUIRED),
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

internal fun QuestionBankEntity.toDomain(questions: List<BankQuestion>): QuestionBank =
    QuestionBank(
        id = QuestionBankId(id),
        name = name,
        sourceFileName = sourceFileName,
        sourceUri = sourceUri,
        subject = subject,
        importStatus = enumValueOrDefault(importStatus, QuestionBankImportStatus.REVIEW_REQUIRED),
        questions = questions,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

internal fun BankQuestionDraft.toEntity(
    id: BankQuestionId,
    now: Long,
): BankQuestionEntity =
    BankQuestionEntity(
        id = id.value,
        bankId = bankId.value,
        stem = stem.trim(),
        optionsText = options.cleaned().encodeTextList(),
        answer = answer?.trim()?.ifEmpty { null },
        analysis = analysis?.trim()?.ifEmpty { null },
        questionType = questionType.name,
        difficulty = difficulty.name,
        tagsText = tags.cleaned().encodeTextList(),
        sourcePage = sourcePage,
        sourceText = sourceText?.trim()?.ifEmpty { null },
        sourceImageUri = sourceImageUri?.trim()?.ifEmpty { null },
        reviewStatus = reviewStatus.name,
        createdAt = now,
        updatedAt = now,
    )

internal fun BankQuestionEntity.toDomain(): BankQuestion =
    BankQuestion(
        id = BankQuestionId(id),
        bankId = QuestionBankId(bankId),
        stem = stem,
        options = optionsText.decodeTextList(),
        answer = answer,
        analysis = analysis,
        questionType = enumValueOrDefault(questionType, BankQuestionType.UNKNOWN),
        difficulty = enumValueOrDefault(difficulty, ExerciseDifficulty.MEDIUM),
        tags = tagsText.decodeTextList(),
        sourcePage = sourcePage,
        sourceText = sourceText,
        sourceImageUri = sourceImageUri,
        reviewStatus = enumValueOrDefault(reviewStatus, QuestionReviewStatus.NEEDS_REVIEW),
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

internal fun ExamSessionEntity.toDomain(attempts: List<ExamAttempt>): ExamSession =
    ExamSession(
        id = ExamSessionId(id),
        bankId = QuestionBankId(bankId),
        title = title,
        seed = seed,
        status = enumValueOrDefault(status, ExamSessionStatus.IN_PROGRESS),
        attempts = attempts,
        createdAtEpochMillis = createdAt,
        completedAtEpochMillis = completedAt,
    )

internal fun ExamSessionSummaryProjection.toDomain(): ExamSessionSummary =
    ExamSessionSummary(
        id = ExamSessionId(id),
        bankId = QuestionBankId(bankId),
        title = title,
        questionCount = questionCount,
        status = enumValueOrDefault(status, ExamSessionStatus.IN_PROGRESS),
        createdAtEpochMillis = createdAt,
        completedAtEpochMillis = completedAt,
    )

internal fun ExamAttemptQuestionProjection.toDomain(): ExamAttempt =
    ExamAttempt(
        id = ExamAttemptId(attemptId),
        sessionId = ExamSessionId(sessionId),
        question = BankQuestion(
            id = BankQuestionId(questionId),
            bankId = QuestionBankId(bankId),
            stem = stem,
            options = optionsText.decodeTextList(),
            answer = answer,
            analysis = analysis,
            questionType = enumValueOrDefault(questionType, BankQuestionType.UNKNOWN),
            difficulty = enumValueOrDefault(difficulty, ExerciseDifficulty.MEDIUM),
            tags = tagsText.decodeTextList(),
            sourcePage = sourcePage,
            sourceText = sourceText,
            sourceImageUri = sourceImageUri,
            reviewStatus = enumValueOrDefault(reviewStatus, QuestionReviewStatus.NEEDS_REVIEW),
            createdAtEpochMillis = questionCreatedAt,
            updatedAtEpochMillis = questionUpdatedAt,
        ),
        orderIndex = orderIndex,
        userAnswer = userAnswer,
        answerRevealed = answerRevealed,
        gradingResult = enumValueOrDefault(gradingResult, ExamGradingResult.NOT_GRADED),
        gradingSource = enumValueOrDefault(gradingSource, ExamGradingSource.NONE),
        gradingFeedback = gradingFeedback,
        submittedAtEpochMillis = submittedAt,
    )

internal fun examAttemptEntity(
    id: ExamAttemptId,
    sessionId: ExamSessionId,
    questionId: BankQuestionId,
    orderIndex: Int,
): ExamAttemptEntity =
    ExamAttemptEntity(
        id = id.value,
        sessionId = sessionId.value,
        questionId = questionId.value,
        orderIndex = orderIndex,
        userAnswer = null,
        answerRevealed = false,
        gradingResult = ExamGradingResult.NOT_GRADED.name,
        gradingSource = ExamGradingSource.NONE.name,
        gradingFeedback = null,
        submittedAt = null,
    )

internal fun examSessionEntity(
    id: ExamSessionId,
    bankId: QuestionBankId,
    title: String,
    seed: Long,
    now: Long,
): ExamSessionEntity =
    ExamSessionEntity(
        id = id.value,
        bankId = bankId.value,
        title = title,
        seed = seed,
        status = ExamSessionStatus.IN_PROGRESS.name,
        createdAt = now,
        completedAt = null,
    )

private fun List<String>.cleaned(): List<String> =
    map { it.trim() }.filter { it.isNotBlank() }

private fun List<String>.encodeTextList(): String =
    joinToString(TEXT_SEPARATOR) { item ->
        item.replace(TEXT_SEPARATOR, " ")
    }

private fun String.decodeTextList(): List<String> =
    split(TEXT_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: default
