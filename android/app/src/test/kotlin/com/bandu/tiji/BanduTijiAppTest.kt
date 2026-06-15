package com.bandu.tiji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduTijiAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `single activity shell renders the application name`() {
        composeRule.setContent {
            BanduTijiApp()
        }

        composeRule.onNodeWithText("伴读题集", substring = true).assertIsDisplayed()
    }
}
