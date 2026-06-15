package com.bandu.tiji.core.testing.fake

import androidx.paging.PagingSource
import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.testing.fixture.collectionFixture
import com.bandu.tiji.core.testing.fixture.errorItemFixture
import com.bandu.tiji.core.testing.fixture.tagFixture
import com.bandu.tiji.domain.tag.CreateTagInput
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeContentRepositoriesTest {
    @Test
    fun `collection flow reflects writes and queued failure is consumed once`() = runTest {
        val initial = collectionFixture()
        val repository = FakeCollectionRepository(listOf(initial))

        repository.observeCollections().test {
            assertThat(awaitItem()).containsExactly(initial)

            val createdId = repository.create("Second")
            assertThat(awaitItem().map { it.id }).containsExactly(initial.id, createdId).inOrder()

            repository.rename(createdId, "Renamed")
            assertThat(awaitItem().last().name).isEqualTo("Renamed")

            repository.failures.enqueue(IllegalStateException("create failed"))
            val failure = runCatching { repository.create("Failed") }.exceptionOrNull()
            assertThat(failure).hasMessageThat().isEqualTo("create failed")

            repository.delete(createdId)
            assertThat(awaitItem()).containsExactly(initial)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error item flow paging and mutations use in-memory state`() = runTest {
        val algebra = tagFixture { id = TagId("algebra") }
        val initial =
            errorItemFixture {
                tags = listOf(algebra)
                questionText = "Solve x + 1 = 2"
            }
        val repository = FakeErrorItemRepository(listOf(initial))

        repository.observe(initial.id).test {
            assertThat(awaitItem()).isEqualTo(initial)

            repository.update(initial.id, ErrorItemPatch(masteryLevel = MasteryLevel.MASTERED))
            assertThat(awaitItem()?.masteryLevel).isEqualTo(MasteryLevel.MASTERED)

            repository.delete(setOf(initial.id))
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        val createdId =
            repository.create(
                ErrorItemDraft(
                    collectionId = CollectionId("collection-2"),
                    image = null,
                    questionText = "Quadratic equation",
                    answerText = "x = 1",
                    analysis = "Factor it",
                    subject = "Math",
                    tagIds = listOf(algebra.id),
                ),
            )
        val loadResult =
            repository
                .page(ErrorItemQuery(keyword = "quadratic", tagIds = setOf(algebra.id)))
                .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))

        assertThat((loadResult as PagingSource.LoadResult.Page).data.single().id).isEqualTo(createdId)

        repository.failures.enqueue(IllegalArgumentException("page failed"))
        val failure = runCatching { repository.page(ErrorItemQuery()) }.exceptionOrNull()
        assertThat(failure).hasMessageThat().isEqualTo("page failed")
    }

    @Test
    fun `tag tree reflects create rename delete and supports failure injection`() = runTest {
        val rootTag = tagFixture { id = TagId("root") }
        val rootNode =
            TagNode(
                tag = rootTag,
                code = "MATH",
                sortOrder = 0,
                linkedErrorItemCount = 0,
                children = emptyList(),
            )
        val repository = FakeTagRepository(listOf(rootNode))

        repository.observeTree("Math").test {
            assertThat(awaitItem().single().tag).isEqualTo(rootTag)

            val childId =
                repository.createCustom(
                    CreateTagInput(name = "Linear", subject = "Math", parentId = rootTag.id),
                )
            assertThat(awaitItem().single().children.single().tag.id).isEqualTo(childId)

            repository.renameCustom(childId, "Linear equations")
            assertThat(awaitItem().single().children.single().tag.name).isEqualTo("Linear equations")

            repository.deleteCustom(childId)
            assertThat(awaitItem().single().children).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }

        repository.failures.enqueue(IllegalStateException("find failed"))
        val failure = runCatching { repository.findTag(rootTag.id) }.exceptionOrNull()
        assertThat(failure).hasMessageThat().isEqualTo("find failed")
        assertThat(repository.findTag(rootTag.id)).isEqualTo(rootTag)
    }
}
