package com.bandu.tiji.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.theme.BanduBorders
import com.bandu.tiji.core.designsystem.theme.BanduElevation
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun BanduBackNavigation(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onBack,
        modifier = modifier.testTag("page-back"),
    ) {
        Text("返回")
    }
}

@Composable
fun BanduPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = BanduElevation.Card,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = BanduSpacing.PageHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(BanduSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navigation?.invoke()
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    actions()
                }
            }
        },
        content = content,
    )
}

@Composable
fun BanduCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(BanduSpacing.PageHorizontal),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(BanduBorders.Standard, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = BanduElevation.Card),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            content = content,
        )
    }
}

@Composable
internal fun BanduStateContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(BanduSpacing.PageHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = BanduSpacing.CardGap,
                alignment = Alignment.CenterVertically,
            ),
            content = content,
        )
    }
}
