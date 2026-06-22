package com.bandu.tiji.feature.profile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `about page shows required product disclosures`() {
        composeRule.setContent {
            BanduTijiTheme {
                AboutScreen(
                    aboutInfo = AboutInfo(versionName = "1.0.0"),
                    onBack = {},
                )
            }
        }

        listOf(
            "伴读题集",
            "1.0.0",
            "隐私说明",
            "开源许可",
            "图标来源",
        ).forEach { text ->
            composeRule.onNodeWithText(text, substring = true).assertExists()
        }
    }
}
