package com.bandu.tiji.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class HomeAcceptanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `AC-HOME-001 four cards open capture library tags and stats`() {
        val actions = mutableListOf<HomeAction>()
        setHomeContent(actions::add)

        listOf(
            "home-card-capture",
            "home-card-library",
            "home-card-tags",
            "home-card-stats",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                HomeAction.OpenDestination(NavigationIntent.OpenCapture),
                HomeAction.OpenDestination(NavigationIntent.OpenLibrary),
                HomeAction.OpenDestination(NavigationIntent.OpenTags),
                HomeAction.OpenDestination(NavigationIntent.OpenStats),
            ).inOrder()
        }
    }

    @Test
    fun `AC-HOME-002 account announcement logout and admin entries are absent`() {
        setHomeContent()

        listOf("账号", "公告", "退出登录", "管理员").forEach { forbiddenText ->
            composeRule.onNodeWithText(forbiddenText, substring = true).assertDoesNotExist()
        }
    }

    private fun setHomeContent(onAction: (HomeAction) -> Unit = {}) {
        composeRule.setContent {
            BanduTijiTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        nickname = "小明",
                        isLoading = false,
                    ),
                    onAction = onAction,
                )
            }
        }
    }
}
