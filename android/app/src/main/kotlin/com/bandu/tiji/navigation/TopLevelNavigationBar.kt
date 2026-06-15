package com.bandu.tiji.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

private val labels = mapOf(
    HomeDestination to "首页",
    DevicesDestination to "设备",
    CaptureDestination to "新增",
    TutorSessionsDestination to "AI辅导",
    ProfileDestination to "我的",
)

@Composable
fun TopLevelNavigationBar(
    selectedDestination: TopLevelDestination,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestinations.ordered.forEach { destination ->
                if (destination == CaptureDestination) {
                    CaptureNavigationButton(
                        selected = selectedDestination == destination,
                        onClick = { onNavigate(destination) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    StandardNavigationButton(
                        destination = destination,
                        selected = selectedDestination == destination,
                        onClick = { onNavigate(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardNavigationButton(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = checkNotNull(labels[destination])
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 56.dp)
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Tab
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun CaptureNavigationButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.defaultMinSize(minWidth = 64.dp, minHeight = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .testTag("capture-navigation-button")
                .size(64.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shape = CircleShape,
                )
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = checkNotNull(labels[CaptureDestination])
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}
