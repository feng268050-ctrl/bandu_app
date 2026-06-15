package com.bandu.tiji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class MainActivityNavigationE2eTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshLaunchOpensHomeWithTopLevelDestinationsInProductOrder() {
        composeRule.onNodeWithText("首页 - 伴读题集").assertIsDisplayed()

        val labels = listOf("首页", "设备", "新增", "AI辅导", "我的")
        val leftEdges = labels.map { label ->
            composeRule.onNodeWithContentDescription(label)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        }

        assertThat(leftEdges).isInStrictOrder()
    }
}
