package com.bandu.tiji.domain.usecase

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.domain.fake.FakeErrorItemRepository
import com.bandu.tiji.domain.usecase.erroritem.CreateErrorItemUseCase
import com.bandu.tiji.domain.usecase.erroritem.DeleteErrorItemsUseCase
import com.bandu.tiji.domain.usecase.erroritem.UpdateErrorItemUseCase
import com.bandu.tiji.domain.usecase.erroritem.UpdateMasteryLevelUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ErrorItemUseCaseTest {
    private val repository = FakeErrorItemRepository()

    private fun validDraft() = ErrorItemDraft(
        collectionId = CollectionId("c1"),
        image = null,
        questionText = "1+1=?",
        answerText = "2",
        analysis = "基础加法",
        subject = "数学",
        tagIds = listOf(TagId("t1")),
    )

    @Test
    fun createErrorItem_persistsDraft() = runTest {
        val result = CreateErrorItemUseCase(repository).invoke(validDraft())

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.createdDrafts).hasSize(1)
    }

    @Test
    fun createErrorItem_mapsRepositoryValidationFailure() = runTest {
        val failingRepository = object : FakeErrorItemRepository() {
            override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
                throw IllegalArgumentException("error_item.invalid")
            }
        }

        val result = CreateErrorItemUseCase(failingRepository).invoke(validDraft())

        assertThat(result.isSuccess).isFalse()
        val error = (result as com.bandu.tiji.core.common.result.AppResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
    }

    @Test
    fun updateMasteryLevel_appliesPatch() = runTest {
        val id = ErrorItemId("e1")
        val result = UpdateMasteryLevelUseCase(repository).invoke(id, MasteryLevel.MASTERED)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.updated.single().second.masteryLevel).isEqualTo(MasteryLevel.MASTERED)
    }

    @Test
    fun updateErrorItem_mapsRepositoryValidationFailure() = runTest {
        val failingRepository = object : FakeErrorItemRepository() {
            override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) {
                throw IllegalArgumentException("error_item.invalid")
            }
        }

        val result = UpdateErrorItemUseCase(failingRepository).invoke(
            ErrorItemId("e1"),
            ErrorItemPatch(answerText = "updated"),
        )

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun deleteErrorItems_requiresNonEmptySelection() = runTest {
        val result = DeleteErrorItemsUseCase(repository).invoke(emptySet())

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun deleteErrorItems_delegatesToRepository() = runTest {
        val ids = setOf(ErrorItemId("e1"), ErrorItemId("e2"))
        val result = DeleteErrorItemsUseCase(repository).invoke(ids)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.deletedIds.single()).isEqualTo(ids)
    }
}
