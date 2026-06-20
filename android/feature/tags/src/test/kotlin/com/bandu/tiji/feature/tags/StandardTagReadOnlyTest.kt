package com.bandu.tiji.feature.tags

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class StandardTagReadOnlyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `standard tags never expose rename or delete actions`() {
        val standard = tagNode(
            id = "standard",
            name = "标准标签",
            subject = "数学",
            isSystem = true,
        )
        val custom = tagNode(
            id = "custom",
            name = "自定义标签",
            subject = "数学",
            isSystem = false,
        )
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = TagsUiState(
                        tree = listOf(standard, custom),
                        isLoading = false,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag("tags-rename-standard").assertDoesNotExist()
        composeRule.onNodeWithTag("tags-delete-standard").assertDoesNotExist()
        composeRule.onNodeWithTag("tags-rename-custom").assertExists()
        composeRule.onNodeWithTag("tags-delete-custom").assertExists()
    }
}
