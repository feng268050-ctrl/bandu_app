package com.bandu.tiji.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.accessibility.banduMinimumTouchTarget
import com.bandu.tiji.core.designsystem.theme.BanduColors
import com.bandu.tiji.core.designsystem.theme.BanduRadii
import com.bandu.tiji.core.designsystem.theme.BanduSpacing
import com.bandu.tiji.core.model.enums.MasteryLevel

@Composable
fun BanduTagChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.banduMinimumTouchTarget(),
        shape = RoundedCornerShape(BanduRadii.Pill),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    )
}

@Composable
fun BanduFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.banduMinimumTouchTarget(),
        shape = RoundedCornerShape(BanduRadii.Pill),
    )
}

@Composable
fun BanduMasteryBadge(
    masteryLevel: MasteryLevel,
    modifier: Modifier = Modifier,
) {
    val style = masteryBadgeStyle(masteryLevel)
    Surface(
        modifier = modifier.semantics {
            contentDescription = "掌握状态：${style.label}"
        },
        shape = RoundedCornerShape(BanduRadii.Pill),
        color = style.containerColor,
        contentColor = style.contentColor,
    ) {
        Text(
            text = style.label,
            modifier = Modifier.padding(
                horizontal = BanduSpacing.CardGap,
                vertical = BanduSpacing.ExtraSmall,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun BanduPagingLoadingItem(
    modifier: Modifier = Modifier,
    message: String = "正在加载更多",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(BanduSpacing.PageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(
            BanduSpacing.Small,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .semantics { contentDescription = message },
            strokeWidth = 2.dp,
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

internal data class MasteryBadgeStyle(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

internal fun masteryBadgeStyle(masteryLevel: MasteryLevel): MasteryBadgeStyle =
    when (masteryLevel) {
        MasteryLevel.NEW -> MasteryBadgeStyle(
            label = "未掌握",
            containerColor = BanduColors.DangerContainer,
            contentColor = BanduColors.Danger,
        )

        MasteryLevel.REVIEWING -> MasteryBadgeStyle(
            label = "复习中",
            containerColor = BanduColors.AccentContainer,
            contentColor = BanduColors.Accent,
        )

        MasteryLevel.MASTERED -> MasteryBadgeStyle(
            label = "已掌握",
            containerColor = BanduColors.SuccessContainer,
            contentColor = BanduColors.Success,
        )
    }
