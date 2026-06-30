package com.bandu.tiji.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ProfileOverviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `overview shows five settings sections and dispatches selection`() {
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(ProfileUiState(), actions::add)
            }
        }

        ProfileSection.entries.forEach { section ->
            composeRule.onNodeWithText(section.title).assertIsDisplayed()
        }
        composeRule.onNodeWithText("学生资料").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.OpenSection(ProfileSection.STUDENT),
            )
        }
    }

    @Test
    fun `overview shows saved student profile summary first`() {
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(
                    ProfileUiState(
                        studentSummary = StudentProfileSummary(
                            nickname = "小明",
                            educationStage = "初中",
                            grade = 2,
                        ),
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("昵称：小明").assertIsDisplayed()
        composeRule.onNodeWithText("教育阶段：初中").assertIsDisplayed()
        composeRule.onNodeWithText("年级：2年级").assertIsDisplayed()
    }
}
