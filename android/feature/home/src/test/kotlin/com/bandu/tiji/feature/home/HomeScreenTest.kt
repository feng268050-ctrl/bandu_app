package com.bandu.tiji.feature.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home shows app name and personalized welcome without web account entries`() {
        setHomeContent(
            HomeUiState(
                nickname = "小明",
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("伴读题集").assertIsDisplayed()
        composeRule.onNodeWithText("你好，小明").assertIsDisplayed()
        composeRule.onNodeWithText("整理错题，持续复习").assertIsDisplayed()
        listOf("账号", "公告", "退出登录", "管理员").forEach { forbiddenText ->
            composeRule.onNodeWithText(forbiddenText, substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun `home shows generic welcome when nickname is absent`() {
        setHomeContent(
            HomeUiState(
                nickname = null,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("欢迎使用伴读题集").assertIsDisplayed()
    }

    @Test
    fun `home four card layout snapshot has full width ordered cards`() {
        setHomeContent(
            HomeUiState(
                nickname = "小明",
                isLoading = false,
            ),
        )

        val cardTags = listOf(
            "home-card-capture",
            "home-card-library",
            "home-card-tags",
            "home-card-stats",
        )
        val bounds = cardTags.map { tag ->
            composeRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        }

        assertThat(bounds.map { it.width }.distinct()).hasSize(1)
        assertThat(bounds.all { it.width > 300f }).isTrue()
        assertThat(bounds.all { it.height >= 80f }).isTrue()
        bounds.zipWithNext().forEach { (previous, next) ->
            assertThat(next.top).isGreaterThan(previous.bottom)
        }
    }

    @Test
    fun `home cards dispatch their navigation intents`() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            BanduTijiTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        nickname = null,
                        isLoading = false,
                    ),
                    onAction = actions::add,
                )
            }
        }

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
    fun `two hundred percent font on narrow screen keeps all card content visible`() {
        val cardTags = listOf(
            "home-card-capture",
            "home-card-library",
            "home-card-tags",
            "home-card-stats",
        )
        val visibleTexts = listOf(
            "上传/拍摄错题",
            "通过拍照或相册整理新错题",
            "查看题集",
            "浏览题集和已保存错题",
            "标签",
            "管理错题分类标签",
            "统计",
            "查看学习与复习进度",
        )
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 2f,
                ),
            ) {
                BanduTijiTheme {
                    HomeScreen(
                        uiState = HomeUiState(
                            nickname = "小明",
                            isLoading = false,
                        ),
                        onAction = {},
                        modifier = Modifier
                            .width(320.dp)
                            .testTag("home-narrow-root"),
                    )
                }
            }
        }

        val rootBounds = composeRule.onNodeWithTag("home-narrow-root")
            .fetchSemanticsNode()
            .boundsInRoot
        cardTags.forEach { tag ->
            val card = composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
            assertThat(card.left).isAtLeast(rootBounds.left)
            assertThat(card.right).isAtMost(rootBounds.right)
        }
        visibleTexts.forEach { text ->
            composeRule.onNodeWithText(text)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `home error state dispatches retry`() {
        val actions = mutableListOf<HomeAction>()
        composeRule.setContent {
            BanduTijiTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        isLoading = false,
                        errorMessage = "无法加载学生资料",
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("无法加载学生资料").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()
        composeRule.runOnIdle {
            assertThat(actions).containsExactly(HomeAction.RetryProfile)
        }
    }

    private fun setHomeContent(uiState: HomeUiState) {
        composeRule.setContent {
            BanduTijiTheme {
                HomeScreen(
                    uiState = uiState,
                    onAction = {},
                )
            }
        }
    }
}
