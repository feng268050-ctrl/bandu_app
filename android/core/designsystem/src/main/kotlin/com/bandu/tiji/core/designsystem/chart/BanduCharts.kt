package com.bandu.tiji.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.theme.BanduColors
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import java.util.Locale
import kotlin.math.max

data class BanduChartDatum(
    val label: String,
    val value: Float,
) {
    init {
        require(label.isNotBlank()) { "Chart labels must not be blank" }
        require(value.isFinite() && value >= 0f) {
            "Chart values must be finite and non-negative"
        }
    }
}

private val ChartPalette = listOf(
    BanduColors.Accent,
    Color(0xFF2563EB),
    BanduColors.Success,
    Color(0xFF7C3AED),
    Color(0xFF0891B2),
    Color(0xFFCA8A04),
    BanduColors.Danger,
    Color(0xFFDB2777),
    Color(0xFF4F46E5),
)

@Composable
fun BanduPieChart(
    data: List<BanduChartDatum>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "暂无统计数据",
) {
    val visibleData = data.filter { it.value > 0f }
    if (visibleData.isEmpty()) {
        BanduChartEmptyState(message = emptyMessage, modifier = modifier)
        return
    }
    val total = visibleData.sumOf { it.value.toDouble() }.toFloat()
    BanduChartContainer(
        data = visibleData,
        chartName = "饼图",
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .size(168.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            var startAngle = -90f
            visibleData.forEachIndexed { index, datum ->
                val sweep = datum.value / total * 360f
                drawArc(
                    color = ChartPalette[index % ChartPalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size,
                )
                startAngle += sweep
            }
        }
        BanduChartLegend(visibleData)
    }
}

@Composable
fun BanduBarChart(
    data: List<BanduChartDatum>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "暂无统计数据",
) {
    if (data.isEmpty()) {
        BanduChartEmptyState(message = emptyMessage, modifier = modifier)
        return
    }
    val largest = max(data.maxOf { it.value }, 1f)
    BanduChartContainer(
        data = data,
        chartName = "柱状图",
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp),
        ) {
            val slotWidth = size.width / data.size
            val barWidth = slotWidth * 0.58f
            data.forEachIndexed { index, datum ->
                val barHeight = size.height * (datum.value / largest)
                drawRoundRect(
                    color = ChartPalette[index % ChartPalette.size],
                    topLeft = Offset(
                        x = slotWidth * index + (slotWidth - barWidth) / 2f,
                        y = size.height - barHeight,
                    ),
                    size = Size(width = barWidth, height = barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }
        }
        BanduChartLegend(data)
    }
}

@Composable
fun BanduTrendChart(
    data: List<BanduChartDatum>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "暂无统计数据",
) {
    if (data.isEmpty()) {
        BanduChartEmptyState(message = emptyMessage, modifier = modifier)
        return
    }
    val largest = max(data.maxOf { it.value }, 1f)
    val pointCenterColor = MaterialTheme.colorScheme.surface
    BanduChartContainer(
        data = data,
        chartName = "趋势图",
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp),
        ) {
            val horizontalPadding = 12.dp.toPx()
            val verticalPadding = 12.dp.toPx()
            val usableWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(0f)
            val usableHeight = (size.height - verticalPadding * 2f).coerceAtLeast(0f)
            val step = if (data.size == 1) 0f else usableWidth / (data.size - 1)
            val points = data.mapIndexed { index, datum ->
                Offset(
                    x = horizontalPadding + step * index,
                    y = verticalPadding + usableHeight * (1f - datum.value / largest),
                )
            }
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = BanduColors.Accent,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            points.forEach { point ->
                drawCircle(
                    color = BanduColors.Accent,
                    radius = 5.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = pointCenterColor,
                    radius = 2.dp.toPx(),
                    center = point,
                )
            }
        }
        BanduChartLegend(data, useDistinctColors = false)
    }
}

@Composable
private fun BanduChartContainer(
    data: List<BanduChartDatum>,
    chartName: String,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) {
                role = Role.Image
                contentDescription = chartDescription(chartName, data)
            },
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        content = content,
    )
}

@Composable
private fun BanduChartLegend(
    data: List<BanduChartDatum>,
    useDistinctColors: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
    ) {
        data.forEachIndexed { index, datum ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (useDistinctColors) {
                                ChartPalette[index % ChartPalette.size]
                            } else {
                                BanduColors.Accent
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = datum.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatChartValue(datum.value),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BanduChartEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .semantics {
                role = Role.Image
                contentDescription = message
            }
            .padding(BanduSpacing.PageHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

internal fun chartDescription(
    chartName: String,
    data: List<BanduChartDatum>,
): String = buildString {
    append(chartName)
    append('：')
    append(
        data.joinToString(separator = "，") { datum ->
            "${datum.label} ${formatChartValue(datum.value)}"
        },
    )
}

internal fun formatChartValue(value: Float): String =
    if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", value)
    }
