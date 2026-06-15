package com.bandu.tiji.domain.usecase

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.domain.fake.FakeTagRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import com.bandu.tiji.domain.usecase.tag.CreateCustomTagUseCase
import com.bandu.tiji.domain.usecase.tag.DeleteCustomTagUseCase
import com.bandu.tiji.domain.usecase.tag.RenameCustomTagUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TagUseCaseTest {
    private val repository = FakeTagRepository()

    @Test
    fun createCustomTag_trimsName() = runTest {
        val result = CreateCustomTagUseCase(repository).invoke(
            CreateTagInput(name = "  函数  ", subject = "数学"),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.created.single().name).isEqualTo("函数")
    }

    @Test
    fun renameCustomTag_rejectsSystemTag() = runTest {
        val id = TagId("sys-1")
        repository.seedSystemTag(id, "数学", "数学")

        val result = RenameCustomTagUseCase(repository).invoke(id, "新名称")

        assertThat(result.isSuccess).isFalse()
        val error = (result as com.bandu.tiji.core.common.result.AppResult.Failure).error
        assertThat((error as AppError.Validation).code).isEqualTo("tag.system_readonly")
        assertThat(repository.renamed).isEmpty()
    }

    @Test
    fun deleteCustomTag_rejectsSystemTag() = runTest {
        val id = TagId("sys-1")
        repository.seedSystemTag(id, "数学", "数学")

        val result = DeleteCustomTagUseCase(repository).invoke(id)

        assertThat(result.isSuccess).isFalse()
        assertThat(repository.deleted).isEmpty()
    }

    @Test
    fun deleteCustomTag_allowsCustomTag() = runTest {
        val id = TagId("custom-1")
        repository.tags[id] = com.bandu.tiji.core.model.tag.TagSummary(id, "自定义", "数学", isSystem = false)

        val result = DeleteCustomTagUseCase(repository).invoke(id)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.deleted).containsExactly(id)
    }
}
