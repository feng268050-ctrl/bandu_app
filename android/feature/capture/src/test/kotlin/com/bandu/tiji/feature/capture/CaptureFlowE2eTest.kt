package com.bandu.tiji.feature.capture

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class CaptureFlowE2eTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `camera capture can proceed through crop analysis review and save`() {
        val repository = FakeErrorItemRepository()
        val viewModel = flowViewModel(repository, "draft-camera")
        setFlowContent(viewModel)

        composeRule.onNodeWithTag(CAMERA_SOURCE_TAG).performClick()
        composeRule.onNodeWithTag(MOCK_CAMERA_CAPTURE_TAG).performClick()
        composeRule.onNodeWithTag(CROP_CONFIRM_TAG).performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(REVIEW_SCREEN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag("${REVIEW_COLLECTION_PREFIX}collection-1").performClick()
        composeRule.onNodeWithTag(REVIEW_SAVE_TAG).performScrollTo().performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(repository.createdDrafts).hasSize(1)
        assertThat(repository.createdDrafts.single().questionText).isEqualTo("求 x")
    }

    @Test
    fun `photo picker result can proceed through crop analysis review and save`() {
        val repository = FakeErrorItemRepository()
        val viewModel = flowViewModel(repository, "draft-photo")
        setFlowContent(viewModel)

        composeRule.onNodeWithTag(PHOTO_SOURCE_TAG).performClick()
        composeRule.runOnIdle {
            viewModel.onAction(CaptureAction.ImageSelected("content://picker/photo"))
        }
        composeRule.onNodeWithTag(CROP_CONFIRM_TAG).performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("${REVIEW_COLLECTION_PREFIX}collection-1").performClick()
        composeRule.onNodeWithTag(REVIEW_SAVE_TAG).performScrollTo().performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(repository.createdDrafts).hasSize(1)
    }

    private fun flowViewModel(
        repository: FakeErrorItemRepository,
        draftId: String,
    ): CaptureViewModel {
        val gateway = FakeAiTutorGateway().apply {
            enqueueAnalyze(CallScript.Return(analyzedQuestion()))
        }
        return CaptureViewModel(
            imageProcessor = RecordingImageProcessor(processed = processedImage()),
            aiGateway = gateway,
            errorItemRepository = repository,
            uuidGenerator = FixedUuidGenerator(draftId),
            initialCollections = listOf(
                CollectionSummary(CollectionId("collection-1"), "期中题集", 0, 1L),
            ),
        )
    }

    private fun setFlowContent(viewModel: CaptureViewModel) {
        composeRule.setContent {
            BanduTijiTheme {
                val state by viewModel.uiState.collectAsState()
                CaptureFlowScreen(
                    uiState = state,
                    onAction = viewModel::onAction,
                    cameraContent = { onCaptured, _ ->
                        Button(
                            onClick = { onCaptured("file://camera.jpg") },
                            modifier = Modifier.testTag(MOCK_CAMERA_CAPTURE_TAG),
                        ) {
                            Text("模拟拍照")
                        }
                    },
                )
            }
        }
    }
}

private const val MOCK_CAMERA_CAPTURE_TAG = "mock-camera-capture"
