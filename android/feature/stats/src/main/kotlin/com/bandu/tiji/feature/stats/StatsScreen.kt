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
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import java.math.RoundingMode

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
                .padding(BanduSpacing.PageHorizontal),
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
                StatsSectionTitle("AI 练习统计")
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
