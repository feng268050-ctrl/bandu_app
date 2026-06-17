package com.bandu.tiji.data.mapper

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CollectionErrorItemMappersTest {
    @Test
    fun `collection entity maps to domain summary and back`() {
        val entity = CollectionEntity(
            id = "collection-1",
            name = "数学错题",
            createdAt = 10L,
            updatedAt = 20L,
        )

        val summary = entity.toDomainSummary(errorItemCount = 3)
        val roundTrip = summary.toEntity(createdAtEpochMillis = entity.createdAt)

        assertThat(summary).isEqualTo(
            CollectionSummary(
                id = CollectionId("collection-1"),
                name = "数学错题",
                errorItemCount = 3,
                updatedAtEpochMillis = 20L,
            ),
        )
        assertThat(roundTrip).isEqualTo(entity)
    }

    @Test
    fun `error item draft maps to entity with storage enum values`() {
        val image = StoredImage(
            relativePath = "images/error-1.jpg",
            sha256Hex = "abc123",
            width = 1200,
            height = 900,
        )
        val draft = ErrorItemDraft(
            collectionId = CollectionId("collection-1"),
            image = image,
            questionText = "1 + 1 = ?",
            answerText = "2",
            analysis = "加法",
            wrongAnswerText = "3",
            mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
            mistakeAnalysis = "看错题目",
            subject = "数学",
            tagIds = listOf(TagId("tag-1"), TagId("tag-2")),
            gradeSemester = "三年级上",
            paperLevel = PaperLevel.A,
            notes = "重点复习",
            masteryLevel = MasteryLevel.REVIEWING,
        )

        val entity = draft.toEntity(ErrorItemId("error-1"), nowEpochMillis = 100L)

        assertThat(entity.id).isEqualTo("error-1")
        assertThat(entity.collectionId).isEqualTo("collection-1")
        assertThat(entity.imagePath).isEqualTo(image.relativePath)
        assertThat(entity.imageSha256).isEqualTo(image.sha256Hex)
        assertThat(entity.mistakeStatus).isEqualTo("WRONG_ATTEMPT")
        assertThat(entity.paperLevel).isEqualTo("A")
        assertThat(entity.masteryLevel).isEqualTo(MasteryLevel.REVIEWING.storageValue)
        assertThat(draft.tagIds.toStorageValues()).containsExactly("tag-1", "tag-2").inOrder()
    }

    @Test
    fun `error item entity maps to domain and preserves non-null text fields`() {
        val tag = TagSummary(TagId("tag-1"), "代数", "数学", isSystem = true)
        val source = ErrorItem(
            id = ErrorItemId("error-1"),
            collectionId = CollectionId("collection-1"),
            image = StoredImage("images/error-1.jpg", "abc123", 1200, 900),
            questionText = "解方程",
            answerText = "x = 1",
            analysis = "移项",
            wrongAnswerText = "x = 2",
            mistakeStatus = MistakeStatus.WRONG_ATTEMPT,
            mistakeAnalysis = "符号错误",
            subject = "数学",
            tags = listOf(tag),
            gradeSemester = "七年级上",
            paperLevel = PaperLevel.B,
            notes = "订正",
            masteryLevel = MasteryLevel.MASTERED,
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
        )

        val roundTrip = source.toEntity().toDomain(tags = listOf(tag))

        assertThat(roundTrip).isEqualTo(source)
    }

    @Test
    fun `error item summary projection maps preview and mastery`() {
        val tag = TagSummary(TagId("tag-1"), "几何", "数学", isSystem = false)
        val projection = ErrorItemSummaryProjection(
            id = "error-1",
            collectionId = "collection-1",
            collectionName = "默认题集",
            imagePath = "images/error-1.jpg",
            questionText = "a".repeat(70),
            masteryLevel = MasteryLevel.NEW.storageValue,
            createdAt = 100L,
            updatedAt = 200L,
        )

        val summary = projection.toDomain(tags = listOf(tag))

        assertThat(summary.id).isEqualTo(ErrorItemId("error-1"))
        assertThat(summary.collectionName).isEqualTo("默认题集")
        assertThat(summary.thumbnailPath).isEqualTo("images/error-1.jpg")
        assertThat(summary.questionPreview).hasLength(60)
        assertThat(summary.tags).containsExactly(tag)
        assertThat(summary.masteryLevel).isEqualTo(MasteryLevel.NEW)
        assertThat(summary.createdAtEpochMillis).isEqualTo(100L)
    }
}
