package com.bandu.tiji.core.model

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.tag.TagSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelTests {
    @Test
    fun ids_rejectBlankValues() {
        assertThrows(IllegalArgumentException::class.java) {
            CollectionId(" ")
        }
    }

    @Test
    fun masteryLevel_roundTripsStorageValue() {
        assertThat(MasteryLevel.fromStorageValue(2)).isEqualTo(MasteryLevel.MASTERED)
    }

    @Test
    fun errorItemDraft_rejectsTooManyTags() {
        val tags = List(6) { TagId("tag-$it") }
        assertThrows(IllegalArgumentException::class.java) {
            ErrorItemDraft(
                collectionId = CollectionId("c1"),
                image = null,
                questionText = "q",
                answerText = "a",
                analysis = "analysis",
                subject = "数学",
                tagIds = tags,
            )
        }
    }

    @Test
    fun errorItemPatch_rejectsBlankQuestionText() {
        assertThrows(IllegalArgumentException::class.java) {
            ErrorItemPatch(questionText = " ")
        }
    }

    @Test
    fun errorItemQuery_defaultsToUnfiltered() {
        val query = ErrorItemQuery()

        assertThat(query.keyword).isEmpty()
        assertThat(query.masteryLevels).isEmpty()
        assertThat(query.collectionId).isNull()
    }

    @Test
    fun exerciseStats_excludesNeedsReviewFromDenominator() {
        val stats = ExerciseStats.fromResults(
            listOf(
                GradeResult.CORRECT,
                GradeResult.INCORRECT,
                GradeResult.NEEDS_REVIEW,
                null,
            ),
        )

        assertThat(stats.totalCount).isEqualTo(3)
        assertThat(stats.gradedCount).isEqualTo(2)
        assertThat(stats.correctCount).isEqualTo(1)
        assertThat(stats.accuracyRate).isWithin(0.0001).of(0.5)
    }

    @Test
    fun navigationIntent_hasNoAndroidDependencies() {
        val intents = listOf(
            NavigationIntent.OpenCapture,
            NavigationIntent.OpenLibrary,
            NavigationIntent.OpenTags,
            NavigationIntent.OpenStats,
        )

        assertThat(intents.map { it::class }).containsNoDuplicates()
    }

    @Test
    fun fixtures_constructSummaryModels() {
        val summary = CollectionSummary(
            id = CollectionId("c1"),
            name = "期中复习",
            errorItemCount = 2,
            updatedAtEpochMillis = 1L,
        )
        val image = StoredImage(
            relativePath = "images/a.jpg",
            sha256Hex = "abc",
            width = 100,
            height = 200,
        )
        val tag = TagSummary(TagId("t1"), "函数", "数学", isSystem = true)

        assertThat(summary.errorItemCount).isEqualTo(2)
        assertThat(image.width).isEqualTo(100)
        assertThat(tag.subject).isEqualTo("数学")
    }

    @Test
    fun mistakeStatus_hasThreeValues() {
        assertThat(MistakeStatus.entries).hasSize(3)
    }

    @Test
    fun errorItemId_acceptsValidUuid() {
        val id = ErrorItemId("550e8400-e29b-41d4-a716-446655440000")
        assertThat(id.value).contains("550e8400")
    }
}
