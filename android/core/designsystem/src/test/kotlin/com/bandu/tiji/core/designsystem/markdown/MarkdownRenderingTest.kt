package com.bandu.tiji.core.designsystem.markdown

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarkdownRenderingTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `streaming markdown refreshes in 250 millisecond batches`() {
        composeRule.mainClock.autoAdvance = false
        var markdown by mutableStateOf("第一步")
        var streaming by mutableStateOf(true)
        var renderedMarkdown = ""

        composeRule.setContent {
            BanduTijiTheme {
                val batched = rememberBatchedMarkdown(markdown, streaming)
                SideEffect {
                    renderedMarkdown = batched
                }
                Text(batched)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle {
            assertThat(renderedMarkdown).isEqualTo("第一步")
        }

        composeRule.runOnIdle {
            markdown = "第一步\n第二步"
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.runOnIdle {
            assertThat(renderedMarkdown).isEqualTo("第一步")
        }
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle {
            assertThat(renderedMarkdown).isEqualTo("第一步\n第二步")
        }

        composeRule.mainClock.autoAdvance = true
        composeRule.runOnIdle {
            markdown = "最终完整回答"
            streaming = false
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertThat(renderedMarkdown).isEqualTo("最终完整回答")
        }
    }

    @Test
    fun `streaming interval is fixed by design contract`() {
        assertThat(MarkdownStreamingRefreshMillis).isEqualTo(250L)
    }
}
