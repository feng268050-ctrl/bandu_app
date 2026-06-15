package com.bandu.tiji.core.designsystem.theme

import android.content.res.Configuration
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `theme maps product colors typography and shapes`() {
        var primary = Color.Unspecified
        var background = Color.Unspecified
        var bodyFont: FontFamily = FontFamily.Default
        var bodySize = 0.sp
        var mediumCornerSize = CornerSize(0.dp)

        composeRule.setContent {
            BanduTijiTheme {
                primary = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
                bodyFont = MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.Default
                bodySize = MaterialTheme.typography.bodyLarge.fontSize
                mediumCornerSize = MaterialTheme.shapes.medium.topStart
            }
        }

        composeRule.runOnIdle {
            assertThat(primary).isEqualTo(BanduColors.Primary)
            assertThat(background).isEqualTo(BanduColors.Background)
            assertThat(bodyFont).isEqualTo(FontFamily.SansSerif)
            assertThat(bodySize).isEqualTo(16.sp)
            assertThat(mediumCornerSize).isEqualTo(CornerSize(BanduRadii.Card))
        }
    }

    @Test
    @Config(sdk = [35], qualifiers = "night")
    fun `theme stays light when system uses dark mode`() {
        var background = Color.Unspecified
        var text = Color.Unspecified
        var uiMode = Configuration.UI_MODE_NIGHT_UNDEFINED

        composeRule.setContent {
            BanduTijiTheme {
                background = MaterialTheme.colorScheme.background
                text = MaterialTheme.colorScheme.onBackground
                uiMode = LocalConfiguration.current.uiMode
            }
        }

        composeRule.runOnIdle {
            assertThat(
                uiMode and Configuration.UI_MODE_NIGHT_MASK,
            ).isEqualTo(Configuration.UI_MODE_NIGHT_YES)
            assertThat(background).isEqualTo(Color.White)
            assertThat(text).isEqualTo(BanduColors.TextPrimary)
        }
    }
}
