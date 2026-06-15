package com.bandu.tiji.core.designsystem.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.chart.BanduBarChart
import com.bandu.tiji.core.designsystem.chart.BanduChartDatum
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduDangerConfirmationDialog
import com.bandu.tiji.core.designsystem.component.BanduFilterChip
import com.bandu.tiji.core.designsystem.component.BanduTagChip
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class BanduAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `interactive components keep at least 48 dp touch targets`() {
        composeRule.setContent {
            BanduTijiTheme {
                Column {
                    BanduTagChip(
                        label = "标签",
                        modifier = Modifier.testTag("tag-chip"),
                    )
                    BanduFilterChip(
                        label = "筛选",
                        selected = false,
                        onClick = {},
                        modifier = Modifier.testTag("filter-chip"),
                    )
                }
            }
        }

        assertMinimumTouchTarget("tag-chip")
        assertMinimumTouchTarget("filter-chip")
    }

    @Test
    fun `danger dialog buttons keep minimum touch targets`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduDangerConfirmationDialog(
                    title = "删除错题",
                    message = "删除后无法恢复",
                    confirmLabel = "删除",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        assertMinimumTouchTargetForText("删除")
        assertMinimumTouchTargetForText("取消")
    }

    @Test
    fun `two hundred percent font scale keeps component text visible and in bounds`() {
        val longLabel = "一次函数与二次函数综合知识点"
        val longBody = "这里展示完整题目说明，文字放大后应自动换行而不是被截断。"

        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 2f,
                ),
            ) {
                BanduTijiTheme {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .testTag("font-scale-container"),
                    ) {
                        BanduCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scaled-card"),
                        ) {
                            androidx.compose.material3.Text(longBody)
                            BanduTagChip(
                                label = longLabel,
                                modifier = Modifier.testTag("scaled-chip"),
                            )
                        }
                        BanduBarChart(
                            data = listOf(
                                BanduChartDatum(longLabel, 12f),
                            ),
                            modifier = Modifier.testTag("scaled-chart"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(longBody).assertIsDisplayed()
        composeRule.onNodeWithTag("scaled-chip").assertIsDisplayed()
        composeRule.onNodeWithTag("scaled-chart").assertIsDisplayed()
        assertInside("scaled-card", "font-scale-container")
        assertInside("scaled-chip", "font-scale-container")
        assertInside("scaled-chart", "font-scale-container")
    }

    private fun assertMinimumTouchTarget(tag: String) {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val minimumPx = with(composeRule.density) { BanduMinimumTouchTarget.toPx() }
        assertThat(bounds.width).isAtLeast(minimumPx)
        assertThat(bounds.height).isAtLeast(minimumPx)
    }

    private fun assertMinimumTouchTargetForText(text: String) {
        val bounds = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        val minimumPx = with(composeRule.density) { BanduMinimumTouchTarget.toPx() }
        assertThat(bounds.width).isAtLeast(minimumPx)
        assertThat(bounds.height).isAtLeast(minimumPx)
    }

    private fun assertInside(childTag: String, parentTag: String) {
        val child = composeRule.onNodeWithTag(childTag).fetchSemanticsNode().boundsInRoot
        val parent = composeRule.onNodeWithTag(parentTag).fetchSemanticsNode().boundsInRoot
        assertThat(child.left).isAtLeast(parent.left)
        assertThat(child.top).isAtLeast(parent.top)
        assertThat(child.right).isAtMost(parent.right)
        assertThat(child.bottom).isAtMost(parent.bottom)
    }
}
