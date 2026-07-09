package com.bandu.tiji.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(
                    ProfileUiState(
                        studentSummary = StudentProfileSummary(
                            nickname = "小明",
                            educationStage = "初中",
                            grade = 2,
                        ),
                        aiDraft = AiConfigurationDraftState(
                            displayName = "Gemini",
                            analysisModel = "gemini-vision",
                            tutorModel = "gemini-tutor",
                        ),
                        isAiConfigurationActive = true,
                        deviceName = "Bandu Pixel",
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("小").assertIsDisplayed()
        composeRule.onNodeWithText("小明").assertIsDisplayed()
        composeRule.onNodeWithText("初中 · 2年级").assertIsDisplayed()
        composeRule.onNodeWithText("模型配置：Gemini · gemini-vision / gemini-tutor")
            .assertIsDisplayed()
        composeRule.onNodeWithText("设备名称：Bandu Pixel").assertIsDisplayed()
        composeRule.onNodeWithText("小明").assertHasNoClickAction()

        composeRule.runOnIdle {
            assertThat(actions).isEmpty()
        }
    }

    @Test
    fun `avatar opens editor without making profile summary clickable`() {
        val actions = mutableListOf<ProfileAction>()
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
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("小明").assertHasNoClickAction()
        composeRule.onNodeWithTag("profile-avatar").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.OpenAvatarEditor,
            )
        }
    }

    @Test
    fun `avatar editor previews colors and saves only after confirm`() {
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(
                    ProfileUiState(
                        studentSummary = StudentProfileSummary(
                            nickname = "小明",
                            educationStage = "初中",
                            grade = 2,
                        ),
                        avatarEditor = ProfileAvatarEditorState(
                            draft = ProfileAvatarUiState(backgroundIndex = 0),
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag("profile-avatar-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("profile-avatar-color-10").assertIsDisplayed()
        composeRule.onNodeWithTag("profile-avatar-color-2").performClick()
        composeRule.onNodeWithText("确认").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.SelectAvatarBackground(2),
                ProfileAction.ConfirmAvatar,
            ).inOrder()
        }
    }

    @Test
    fun `avatar editor image choice launches picker before confirmation`() {
        val actions = mutableListOf<ProfileAction>()
        composeRule.setContent {
            BanduTijiTheme {
                ProfileScreen(
                    ProfileUiState(
                        studentSummary = StudentProfileSummary(
                            nickname = "小明",
                            educationStage = "初中",
                            grade = 2,
                        ),
                        avatarEditor = ProfileAvatarEditorState(
                            draft = ProfileAvatarUiState(backgroundIndex = 0),
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag("profile-avatar-image-choice").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                ProfileAction.RequestAvatarImagePicker,
            )
        }
    }
}
