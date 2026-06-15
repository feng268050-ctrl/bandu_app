package com.bandu.tiji.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.chart.BanduChartDatum
import com.bandu.tiji.core.designsystem.chart.BanduPieChart
import com.bandu.tiji.core.designsystem.chart.BanduTrendChart
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.stats.MonthlyCount
import java.math.RoundingMode
import java.time.YearMonth

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onAction: (StatsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "统计",
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal)
                .testTag("stats-list"),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.Large),
        ) {
            item {
                StatsSectionTitle("错题统计")
            }
            item {
                StatMetricCard(
                    label = "错题总数",
                    value = uiState.wrongItemStats.totalCount.toString(),
                )
            }
            item {
                StatMetricCard(
                    label = "已掌握",
                    value = uiState.wrongItemStats.masteredCount.toString(),
                )
            }
            item {
                StatMetricCard(
                    label = "掌握率",
                    value = formatPercentage(uiState.wrongItemStats.masteryRate),
                )
            }
            item {
                StatsSectionTitle("错题学科分布")
            }
            item {
                BanduPieChart(
                    data = uiState.wrongItemStats.subjectCounts
                        .toSortedMap()
                        .map { (subject, count) ->
                            BanduChartDatum(subject, count.toFloat())
                        },
                    emptyMessage = "暂无错题学科数据",
                )
            }
            item {
                StatsSectionTitle("最近 6 个月新增趋势")
            }
            item {
                BanduTrendChart(
                    data = fillLastSixMonths(uiState.wrongItemStats.monthlyNewCounts)
                        .map { month ->
                            BanduChartDatum(
                                label = formatMonthLabel(month),
                                value = month.count.toFloat(),
                            )
                        },
                    emptyMessage = "暂无新增错题趋势",
                )
            }
            item {
                StatsSectionTitle("AI 练习统计")
            }
            item {
                StatMetricCard(
                    label = "练习总数",
                    value = uiState.exerciseStats.totalCount.toString(),
                )
            }
            item {
                StatMetricCard(
                    label = "正确率",
                    value = formatPercentage(uiState.exerciseStats.accuracyRate),
                )
            }
            item {
                StatMetricCard(
                    label = "最近 6 个月活跃天数",
                    value = "${uiState.exerciseStats.activeDaysLastSixMonths} 天",
                )
            }
        }
    }
}

@Composable
private fun StatsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    BanduCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

internal fun formatPercentage(rate: Double): String {
    val percentage = rate.coerceIn(0.0, 1.0) * 100.0
    return percentage.toBigDecimal()
        .setScale(if (percentage % 1.0 == 0.0) 0 else 1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString() + "%"
}

internal fun fillLastSixMonths(counts: List<MonthlyCount>): List<MonthlyCount> {
    val countsByMonth = counts
        .filter { it.month in 1..12 }
        .groupBy { YearMonth.of(it.year, it.month) }
        .mapValues { (_, entries) -> entries.sumOf(MonthlyCount::count) }
    val endMonth = countsByMonth.keys.maxOrNull() ?: return emptyList()

    return (5L downTo 0L).map { monthsAgo ->
        val month = endMonth.minusMonths(monthsAgo)
        MonthlyCount(
            year = month.year,
            month = month.monthValue,
            count = countsByMonth[month] ?: 0,
        )
    }
}

internal fun formatMonthLabel(month: MonthlyCount): String =
    "${month.year}年${month.month}月"
