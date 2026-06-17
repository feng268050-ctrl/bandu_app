package com.bandu.tiji.data.mapper

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection

internal fun CollectionEntity.toDomainSummary(errorItemCount: Int): CollectionSummary =
    CollectionSummary(
        id = CollectionId(id),
        name = name,
        errorItemCount = errorItemCount,
        updatedAtEpochMillis = updatedAt,
    )

internal fun CollectionSummary.toEntity(createdAtEpochMillis: Long): CollectionEntity =
    CollectionEntity(
        id = id.value,
        name = name,
        createdAt = createdAtEpochMillis,
        updatedAt = updatedAtEpochMillis,
    )

internal fun ErrorItemDraft.toEntity(
    id: ErrorItemId,
    nowEpochMillis: Long,
): ErrorItemEntity =
    ErrorItemEntity(
        id = id.value,
        collectionId = collectionId.value,
        imagePath = image?.relativePath,
        imageSha256 = image?.sha256Hex,
        imageWidth = image?.width,
        imageHeight = image?.height,
        questionText = questionText,
        answerText = answerText,
        analysis = analysis,
        wrongAnswerText = wrongAnswerText,
        mistakeStatus = mistakeStatus.name,
        mistakeAnalysis = mistakeAnalysis,
        subject = subject,
        gradeSemester = gradeSemester,
        paperLevel = paperLevel?.name,
        notes = notes,
        masteryLevel = masteryLevel.storageValue,
        createdAt = nowEpochMillis,
        updatedAt = nowEpochMillis,
    )

internal fun ErrorItem.toEntity(): ErrorItemEntity =
    ErrorItemEntity(
        id = id.value,
        collectionId = collectionId.value,
        imagePath = image?.relativePath,
        imageSha256 = image?.sha256Hex,
        imageWidth = image?.width,
        imageHeight = image?.height,
        questionText = questionText,
        answerText = answerText,
        analysis = analysis,
        wrongAnswerText = wrongAnswerText,
        mistakeStatus = mistakeStatus.name,
        mistakeAnalysis = mistakeAnalysis,
        subject = subject,
        gradeSemester = gradeSemester,
        paperLevel = paperLevel?.name,
        notes = notes,
        masteryLevel = masteryLevel.storageValue,
        createdAt = createdAtEpochMillis,
        updatedAt = updatedAtEpochMillis,
    )

internal fun ErrorItemEntity.toDomain(tags: List<TagSummary>): ErrorItem =
    ErrorItem(
        id = ErrorItemId(id),
        collectionId = CollectionId(collectionId),
        image = toStoredImage(),
        questionText = questionText,
        answerText = answerText,
        analysis = analysis,
        wrongAnswerText = wrongAnswerText,
        mistakeStatus = MistakeStatus.valueOf(mistakeStatus),
        mistakeAnalysis = mistakeAnalysis,
        subject = subject,
        tags = tags,
        gradeSemester = gradeSemester,
        paperLevel = paperLevel?.let(PaperLevel::valueOf),
        notes = notes,
        masteryLevel = MasteryLevel.fromStorageValue(masteryLevel),
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

internal fun ErrorItemSummaryProjection.toDomain(tags: List<TagSummary>): ErrorItemSummary =
    ErrorItemSummary(
        id = ErrorItemId(id),
        collectionId = CollectionId(collectionId),
        collectionName = collectionName,
        thumbnailPath = imagePath,
        questionPreview = questionText.take(QUESTION_PREVIEW_MAX_CHARS),
        tags = tags,
        masteryLevel = MasteryLevel.fromStorageValue(masteryLevel),
        createdAtEpochMillis = createdAt,
    )

internal fun List<TagId>.toStorageValues(): List<String> = map(TagId::value)

private fun ErrorItemEntity.toStoredImage(): StoredImage? {
    val path = imagePath ?: return null
    return StoredImage(
        relativePath = path,
        sha256Hex = imageSha256.orEmpty(),
        width = imageWidth ?: 0,
        height = imageHeight ?: 0,
    )
}

private const val QUESTION_PREVIEW_MAX_CHARS = 60
