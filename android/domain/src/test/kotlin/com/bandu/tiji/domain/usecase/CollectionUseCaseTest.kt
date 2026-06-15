package com.bandu.tiji.domain.usecase

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.domain.fake.FakeCollectionRepository
import com.bandu.tiji.domain.usecase.collection.CreateCollectionUseCase
import com.bandu.tiji.domain.usecase.collection.DeleteCollectionUseCase
import com.bandu.tiji.domain.usecase.collection.RenameCollectionUseCase
import com.bandu.tiji.domain.validation.CollectionNameValidator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CollectionUseCaseTest {
    private val repository = FakeCollectionRepository()

    @Test
    fun createCollection_trimsAndPersistsName() = runTest {
        val result = CreateCollectionUseCase(repository).invoke("  期中复习  ")

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.createdNames).containsExactly("期中复习")
    }

    @Test
    fun createCollection_rejectsBlankName() = runTest {
        val result = CreateCollectionUseCase(repository).invoke("   ")

        assertThat(result.getOrNull()).isNull()
        val error = (result as com.bandu.tiji.core.common.result.AppResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Validation::class.java)
        assertThat((error as AppError.Validation).code).isEqualTo(CollectionNameValidator.CODE_BLANK)
    }

    @Test
    fun renameCollection_rejectsTooLongName() = runTest {
        val id = CollectionId("c1")
        val result = RenameCollectionUseCase(repository).invoke(id, "x".repeat(65))

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun deleteCollection_delegatesToRepository() = runTest {
        val id = CollectionId("c1")
        val result = DeleteCollectionUseCase(repository).invoke(id)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.deletedIds).containsExactly(id)
    }
}
