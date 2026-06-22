package com.bandu.tiji.feature.tutor

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fake.TutorStreamScript
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class TutorProcessRecoveryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `completed conversation survives view model recreation`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Events(
                    listOf(
                        AiStreamEvent.Delta("持久化回答"),
                        AiStreamEvent.Completed,
                    ),
                ),
            )
        }
        val first = TutorSessionViewModel(
            repository,
            session.id,
            gateway,
            FakeExerciseRepository(),
        )
        advanceUntilIdle()
        first.onAction(TutorSessionAction.UpdateInput("持久化问题"))
        first.onAction(TutorSessionAction.Send)
        advanceUntilIdle()

        val recreated = TutorSessionViewModel(
            repository,
            session.id,
            FakeAiTutorGateway(),
            FakeExerciseRepository(),
        )
        advanceUntilIdle()

        assertThat(recreated.uiState.value.session?.messages?.map { it.content })
            .containsExactly("持久化问题", "持久化回答")
            .inOrder()
        assertThat(recreated.uiState.value.isStreaming).isFalse()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TutorLongSessionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `two hundred message conversation scrolls to the end`() {
        val session = sessionFixture { messages = longMessages() }
        composeRule.setContent {
            BanduTijiTheme {
                TutorSessionScreen(
                    uiState = TutorSessionUiState(session = session, isLoading = false),
                    onAction = {},
                    markdownRenderer = { markdown, modifier, streaming ->
                        Text("$streaming:$markdown", modifier)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("tutor-session-messages").performScrollToIndex(199)
        composeRule.onNodeWithText("false:消息 199").assertIsDisplayed()
    }

    @Test
    fun `streaming markdown is delegated with streaming mode`() {
        val session = sessionFixture()
        composeRule.setContent {
            BanduTijiTheme {
                TutorSessionScreen(
                    uiState = TutorSessionUiState(
                        session = session,
                        streamingText = "增量 \$x^2\$",
                        isLoading = false,
                        isStreaming = true,
                    ),
                    onAction = {},
                    markdownRenderer = { markdown, modifier, streaming ->
                        Text("$streaming:$markdown", modifier)
                    },
                )
            }
        }

        composeRule.onNodeWithText("true:增量 \$x^2\$").assertIsDisplayed()
    }

    private fun longMessages(): List<TutorMessage> =
        (0 until 200).map { index ->
            TutorMessage(
                id = TutorMessageId("message-$index"),
                role = if (index % 2 == 0) {
                    TutorMessageRole.USER
                } else {
                    TutorMessageRole.ASSISTANT
                },
                content = "消息 $index",
                status = TutorMessageStatus.COMPLETE,
                sequence = index,
                createdAtEpochMillis = index.toLong(),
            )
        }
}
