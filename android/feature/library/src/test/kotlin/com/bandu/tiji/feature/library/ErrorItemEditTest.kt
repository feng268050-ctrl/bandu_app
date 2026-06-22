package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `save editor persists every editable field and survives view model recreation`() = runTest {
        val original = detailItem()
        val repository = FakeErrorItemRepository(listOf(original))
        val viewModel = ErrorItemDetailViewModel(repository, original.id)
        advanceUntilIdle()

        val replacementImage = StoredImage(
            relativePath = "images/replacement.webp",
            sha256Hex = "abc123",
            width = 1200,
            height = 900,
            thumbnailRelativePath = "thumbnails/replacement.webp",
        )
        val replacementCollection = CollectionId("collection-2")
        val replacementTags = setOf(TagId("tag-2"), TagId("tag-3"))

        viewModel.onAction(ErrorItemDetailAction.OpenEditor)
        viewModel.onAction(
            ErrorItemDetailAction.UpdateEditor(
                checkNotNull(viewModel.uiState.value.editor).copy(
                    image = replacementImage,
                    questionText = "  新题目  ",
                    answerText = "  新答案  ",
                    analysis = "  新解析  ",
                    wrongAnswerText = "  新错误答案  ",
                    mistakeStatus = MistakeStatus.NOT_ATTEMPTED,
                    mistakeAnalysis = "  新错误分析  ",
                    collectionId = replacementCollection,
                    subject = "  物理  ",
                    tagIds = replacementTags,
                    gradeSemester = "  九年级上  ",
                    paperLevel = PaperLevel.B,
                    notes = "  新笔记  ",
                ),
            ),
        )
        viewModel.onAction(ErrorItemDetailAction.SaveEditor)
        advanceUntilIdle()

        val patch = repository.updatedItems.single().second
        assertThat(patch.collectionId).isEqualTo(replacementCollection)
        assertThat(patch.image).isEqualTo(replacementImage)
        assertThat(patch.questionText).isEqualTo("新题目")
        assertThat(patch.answerText).isEqualTo("新答案")
        assertThat(patch.analysis).isEqualTo("新解析")
        assertThat(patch.wrongAnswerText).isEqualTo("新错误答案")
        assertThat(patch.mistakeStatus).isEqualTo(MistakeStatus.NOT_ATTEMPTED)
        assertThat(patch.mistakeAnalysis).isEqualTo("新错误分析")
        assertThat(patch.subject).isEqualTo("物理")
        assertThat(patch.tagIds).containsExactlyElementsIn(replacementTags)
        assertThat(patch.gradeSemester).isEqualTo("九年级上")
        assertThat(patch.paperLevel).isEqualTo(PaperLevel.B)
        assertThat(patch.notes).isEqualTo("新笔记")
        assertThat(viewModel.uiState.value.editor).isNull()

        val recreated = ErrorItemDetailViewModel(repository, original.id)
        advanceUntilIdle()

        with(checkNotNull(recreated.uiState.value.item)) {
            assertThat(collectionId).isEqualTo(replacementCollection)
            assertThat(image).isEqualTo(replacementImage)
            assertThat(questionText).isEqualTo("新题目")
            assertThat(answerText).isEqualTo("新答案")
            assertThat(analysis).isEqualTo("新解析")
            assertThat(wrongAnswerText).isEqualTo("新错误答案")
            assertThat(mistakeStatus).isEqualTo(MistakeStatus.NOT_ATTEMPTED)
            assertThat(mistakeAnalysis).isEqualTo("新错误分析")
            assertThat(subject).isEqualTo("物理")
            assertThat(tags.map { it.id }).containsExactlyElementsIn(replacementTags)
            assertThat(gradeSemester).isEqualTo("九年级上")
            assertThat(paperLevel).isEqualTo(PaperLevel.B)
            assertThat(notes).isEqualTo("新笔记")
        }
    }

    @Test
    fun `more than five tags is rejected without repository update`() = runTest {
        val item = detailItem()
        val repository = FakeErrorItemRepository(listOf(item))
        val viewModel = ErrorItemDetailViewModel(repository, item.id)
        advanceUntilIdle()

        viewModel.onAction(ErrorItemDetailAction.OpenEditor)
        viewModel.onAction(
            ErrorItemDetailAction.UpdateEditor(
                checkNotNull(viewModel.uiState.value.editor).copy(
                    tagIds = (1..6).map { TagId("tag-$it") }.toSet(),
                ),
            ),
        )
        viewModel.onAction(ErrorItemDetailAction.SaveEditor)
        advanceUntilIdle()

        assertThat(repository.updatedItems).isEmpty()
        assertThat(viewModel.uiState.value.editorErrorMessage)
            .isEqualTo("每道错题最多选择 5 个标签")
    }

    @Test
    fun `image replacement request emits picker effect and updates draft`() = runTest {
        val item = detailItem()
        val viewModel = ErrorItemDetailViewModel(
            FakeErrorItemRepository(listOf(item)),
            item.id,
        )
        advanceUntilIdle()
        viewModel.onAction(ErrorItemDetailAction.OpenEditor)

        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ErrorItemDetailAction.RequestImageReplacement)
        assertThat(effect.await()).isEqualTo(ErrorItemDetailEffect.SelectReplacementImage)

        val image = StoredImage("images/new.webp", "hash", 100, 200)
        viewModel.onAction(ErrorItemDetailAction.ReplaceImage(image))
        assertThat(viewModel.uiState.value.editor?.image).isEqualTo(image)
    }

    @Test
    fun `tag toggle keeps selection at five and allows deselection`() {
        val selected = (1..5).map { TagId("tag-$it") }.toSet()

        assertThat(toggleEditorTag(selected, TagId("tag-6")))
            .containsExactlyElementsIn(selected)
        assertThat(toggleEditorTag(selected, TagId("tag-3")))
            .containsExactlyElementsIn(selected - TagId("tag-3"))
        assertThat(toggleEditorTag(selected - TagId("tag-3"), TagId("tag-6")))
            .containsExactlyElementsIn((selected - TagId("tag-3")) + TagId("tag-6"))
    }
}
