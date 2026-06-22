package com.bandu.tiji.feature.tutor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class TutorSessionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `session cards show title binding and update time`() {
        val bound = summary(
            id = "bound",
            title = "一元二次方程",
            errorItemId = ErrorItemId("error-1"),
        )
        val unbound = summary(
            id = "unbound",
            title = "自由提问",
            errorItemId = null,
        )

        composeRule.setContent {
            BanduTijiTheme {
                TutorSessionsScreen(
                    uiState = TutorSessionsUiState(
                        sessions = listOf(bound, unbound),
                        isLoading = false,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("一元二次方程").assertIsDisplayed()
        composeRule.onNodeWithText("自由提问").assertIsDisplayed()
        composeRule.onNodeWithText("已绑定错题").assertIsDisplayed()
        composeRule.onNodeWithText("未绑定错题").assertIsDisplayed()
        composeRule.onAllNodesWithText("更新于", substring = true).apply {
            assertThat(fetchSemanticsNodes()).hasSize(2)
        }
    }

    @Test
    fun `clicking session dispatches open action`() {
        val session = summary("session-1", "函数辅导", null)
        val actions = mutableListOf<TutorSessionsAction>()
        composeRule.setContent {
            BanduTijiTheme {
                TutorSessionsScreen(
                    uiState = TutorSessionsUiState(
                        sessions = listOf(session),
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("函数辅导").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                TutorSessionsAction.OpenSession(session.id),
            )
        }
    }

    private fun summary(
        id: String,
        title: String,
        errorItemId: ErrorItemId?,
    ) = TutorSessionSummary(
        id = TutorSessionId(id),
        title = title,
        errorItemId = errorItemId,
        updatedAtEpochMillis = 1_700_000_000_000L,
    )
}
