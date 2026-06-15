package com.bandu.tiji.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TopLevelNavigationBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `bar renders the five required entries and a 64 dp capture button`() {
        composeRule.setContent {
            BanduTijiTheme {
                TopLevelNavigationBar(
                    selectedDestination = HomeDestination,
                    onNavigate = {},
                )
            }
        }

        listOf("首页", "设备", "新增", "AI辅导", "我的").forEach { label ->
            composeRule.onNodeWithContentDescription(label).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("capture-navigation-button")
            .assertWidthIsEqualTo(64.dp)
    }

    @Test
    fun `focused operations hide bottom navigation`() {
        assertThat(shouldShowBottomNavigation(HomeDestination)).isTrue()
        assertThat(shouldShowBottomNavigation(FocusedOperationDestination)).isFalse()
    }
}
