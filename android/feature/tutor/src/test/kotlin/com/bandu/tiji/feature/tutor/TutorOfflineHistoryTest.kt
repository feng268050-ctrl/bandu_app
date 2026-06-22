package com.bandu.tiji.feature.tutor

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TutorOfflineHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `session history loads from repository without invoking network`() = runTest {
        val session = sessionFixture {
            messages = historyMessages()
        }
        val repository = FakeTutorRepository(listOf(session))

        val viewModel = TutorSessionViewModel(
            repository,
            session.id,
            FakeAiTutorGateway(),
            FakeExerciseRepository(),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.messages).containsExactlyElementsIn(
            historyMessages(),
        ).inOrder()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TutorOfflineHistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `history delegates markdown tables and latex to rich renderer`() {
        val session = sessionFixture {
            messages = historyMessages()
        }
        composeRule.setContent {
            BanduTijiTheme {
                TutorSessionScreen(
                    uiState = TutorSessionUiState(
                        session = session,
                        isLoading = false,
                    ),
                    onAction = {},
                    markdownRenderer = { markdown, modifier, streaming ->
                        Text("rich:$streaming:$markdown", modifier)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("tutor-message-user").assertExists()
        composeRule.onNodeWithTag("tutor-message-assistant").assertExists()
        composeRule.onNodeWithText("rich:false:| x | y |", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("rich:false:\$\$x^2\$\$", substring = true)
            .assertIsDisplayed()
    }
}

private fun historyMessages() = listOf(
    TutorMessage(
        id = TutorMessageId("user"),
        role = TutorMessageRole.USER,
        content = "| x | y |\n|---|---|\n| 1 | 2 |",
        status = TutorMessageStatus.COMPLETE,
        sequence = 0,
        createdAtEpochMillis = 1L,
    ),
    TutorMessage(
        id = TutorMessageId("assistant"),
        role = TutorMessageRole.ASSISTANT,
        content = "\$\$x^2\$\$",
        status = TutorMessageStatus.COMPLETE,
        sequence = 1,
        createdAtEpochMillis = 2L,
    ),
)
