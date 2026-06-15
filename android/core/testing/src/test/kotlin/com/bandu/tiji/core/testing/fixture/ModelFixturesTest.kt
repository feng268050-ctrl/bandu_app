package com.bandu.tiji.core.testing.fixture

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelFixturesTest {
    @Test
    fun `default fixtures construct valid models`() {
        val collection = collectionFixture()
        val tag = tagFixture()
        val errorItem = errorItemFixture()
        val session = sessionFixture()
        val exercise = exerciseFixture()

        assertThat(collection.id.value).isNotEmpty()
        assertThat(tag.id.value).isNotEmpty()
        assertThat(errorItem.tags).containsExactly(tag)
        assertThat(session.messages).isEmpty()
        assertThat(exercise.sessionId).isEqualTo(session.id)
    }

    @Test
    fun `fixture builders override relevant model fields`() {
        val collectionId = CollectionId("custom-collection")
        val errorItemId = ErrorItemId("custom-error")
        val customTag = tagFixture {
            id = TagId("custom-tag")
            name = "Geometry"
        }
        val errorItem = errorItemFixture {
            id = errorItemId
            this.collectionId = collectionId
            tags = listOf(customTag)
            masteryLevel = MasteryLevel.MASTERED
            paperLevel = PaperLevel.A
        }
        val exercise = exerciseFixture {
            sessionId = TutorSessionId("custom-session")
            sourceErrorItemId = errorItem.id
            difficulty = ExerciseDifficulty.HARD
        }

        assertThat(errorItem.collectionId).isEqualTo(collectionId)
        assertThat(errorItem.tags).containsExactly(customTag)
        assertThat(errorItem.masteryLevel).isEqualTo(MasteryLevel.MASTERED)
        assertThat(errorItem.paperLevel).isEqualTo(PaperLevel.A)
        assertThat(exercise.sourceErrorItemId).isEqualTo(errorItemId)
        assertThat(exercise.difficulty).isEqualTo(ExerciseDifficulty.HARD)
    }

    @Test
    fun `fixture defaults do not share mutable collections`() {
        val first = errorItemFixture()
        val second = errorItemFixture()

        assertThat(first.tags).isNotSameInstanceAs(second.tags)
    }
}
