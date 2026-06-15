package com.bandu.tiji.core.designsystem.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BanduChartsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all charts show an accessible empty state`() {
        composeRule.setContent {
            BanduTijiTheme {
                Column {
                    BanduPieChart(emptyList(), emptyMessage = "饼图暂无数据")
                    BanduBarChart(emptyList(), emptyMessage = "柱状图暂无数据")
                    BanduTrendChart(emptyList(), emptyMessage = "趋势图暂无数据")
                }
            }
        }

        listOf("饼图暂无数据", "柱状图暂无数据", "趋势图暂无数据").forEach { message ->
            composeRule.onNodeWithContentDescription(message).assertIsDisplayed()
            composeRule.onNodeWithText(message).assertIsDisplayed()
        }
    }

    @Test
    fun `six month data is rendered with labels values and summaries`() {
        val months = listOf(
            BanduChartDatum("1月", 4f),
            BanduChartDatum("2月", 7f),
            BanduChartDatum("3月", 3f),
            BanduChartDatum("4月", 9f),
            BanduChartDatum("5月", 6f),
            BanduChartDatum("6月", 8f),
        )
        composeRule.setContent {
            BanduTijiTheme {
                Column {
                    BanduBarChart(months)
                    BanduTrendChart(months)
                }
            }
        }

        val summary = "1月 4，2月 7，3月 3，4月 9，5月 6，6月 8"
        composeRule.onNodeWithContentDescription("柱状图：$summary").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("趋势图：$summary").assertIsDisplayed()
        months.forEach { month ->
            composeRule.onAllNodesWithText(month.label)
                .fetchSemanticsNodes()
                .let { assertThat(it).hasSize(2) }
        }
    }

    @Test
    fun `pie chart ignores zero slices but keeps visible distribution`() {
        composeRule.setContent {
            BanduTijiTheme {
                BanduPieChart(
                    listOf(
                        BanduChartDatum("数学", 6f),
                        BanduChartDatum("物理", 0f),
                        BanduChartDatum("化学", 4f),
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("饼图：数学 6，化学 4")
            .assertIsDisplayed()
        composeRule.onNodeWithText("物理").assertDoesNotExist()
    }

    @Test
    fun `chart data rejects invalid values`() {
        assertThat(
            runCatching { BanduChartDatum("数学", Float.NaN) }.exceptionOrNull(),
        ).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(
            runCatching { BanduChartDatum("数学", -1f) }.exceptionOrNull(),
        ).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `chart values use stable compact formatting`() {
        assertThat(formatChartValue(12f)).isEqualTo("12")
        assertThat(formatChartValue(12.25f)).isEqualTo("12.3")
        assertThat(
            chartDescription(
                "饼图",
                listOf(BanduChartDatum("数学", 12.25f)),
            ),
        ).isEqualTo("饼图：数学 12.3")
    }
}
