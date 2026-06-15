package com.bandu.tiji.core.model.erroritem

import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary

data class ErrorItem(
    val id: ErrorItemId,
    val collectionId: CollectionId,
    val image: StoredImage?,
    val questionText: String,
    val answerText: String,
    val analysis: String,
    val wrongAnswerText: String,
    val mistakeStatus: MistakeStatus,
    val mistakeAnalysis: String,
    val subject: String,
    val tags: List<TagSummary>,
    val gradeSemester: String?,
    val paperLevel: PaperLevel?,
    val notes: String,
    val masteryLevel: MasteryLevel,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class ErrorItemSummary(
    val id: ErrorItemId,
    val collectionId: CollectionId,
    val collectionName: String,
    val thumbnailPath: String?,
    val questionPreview: String,
    val tags: List<TagSummary>,
    val masteryLevel: MasteryLevel,
    val createdAtEpochMillis: Long,
)

data class ErrorItemDraft(
    val collectionId: CollectionId,
    val image: StoredImage?,
    val questionText: String,
    val answerText: String,
    val analysis: String,
    val wrongAnswerText: String = "",
    val mistakeStatus: MistakeStatus = MistakeStatus.UNKNOWN,
    val mistakeAnalysis: String = "",
    val subject: String,
    val tagIds: List<TagId>,
    val gradeSemester: String? = null,
    val paperLevel: PaperLevel? = null,
    val notes: String = "",
    val masteryLevel: MasteryLevel = MasteryLevel.NEW,
) {
    init {
        require(questionText.isNotBlank()) { "questionText must not be blank" }
        require(answerText.isNotBlank()) { "answerText must not be blank" }
        require(analysis.isNotBlank()) { "analysis must not be blank" }
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(tagIds.size <= MAX_TAG_COUNT) { "At most $MAX_TAG_COUNT tags are allowed" }
    }

    companion object {
        const val MAX_TAG_COUNT = 5
    }
}

data class ErrorItemPatch(
    val collectionId: CollectionId? = null,
    val image: StoredImage? = null,
    val questionText: String? = null,
    val answerText: String? = null,
    val analysis: String? = null,
    val wrongAnswerText: String? = null,
    val mistakeStatus: MistakeStatus? = null,
    val mistakeAnalysis: String? = null,
    val subject: String? = null,
    val tagIds: List<TagId>? = null,
    val gradeSemester: String? = null,
    val paperLevel: PaperLevel? = null,
    val notes: String? = null,
    val masteryLevel: MasteryLevel? = null,
) {
    init {
        questionText?.let { require(it.isNotBlank()) { "questionText must not be blank" } }
        answerText?.let { require(it.isNotBlank()) { "answerText must not be blank" } }
        analysis?.let { require(it.isNotBlank()) { "analysis must not be blank" } }
        subject?.let { require(it.isNotBlank()) { "subject must not be blank" } }
        tagIds?.let { require(it.size <= ErrorItemDraft.MAX_TAG_COUNT) { "At most ${ErrorItemDraft.MAX_TAG_COUNT} tags are allowed" } }
    }
}

data class ErrorItemQuery(
    val collectionId: CollectionId? = null,
    val keyword: String = "",
    val masteryLevels: Set<MasteryLevel> = emptySet(),
    val createdAfterEpochMillis: Long? = null,
    val tagIds: Set<TagId> = emptySet(),
    val gradeSemester: String? = null,
    val paperLevels: Set<PaperLevel> = emptySet(),
)
