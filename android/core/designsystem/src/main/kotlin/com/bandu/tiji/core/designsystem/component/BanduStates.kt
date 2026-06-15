package com.bandu.tiji.core.designsystem.component

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bandu.tiji.core.designsystem.accessibility.banduMinimumTouchTarget

@Composable
fun BanduEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    BanduStateContainer(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        description?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.banduMinimumTouchTarget(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun BanduLoadingState(
    modifier: Modifier = Modifier,
    message: String = "正在加载",
) {
    BanduStateContainer(modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = message },
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun BanduErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "加载失败",
    retryLabel: String = "重试",
) {
    BanduStateContainer(modifier = modifier) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.banduMinimumTouchTarget(),
        ) {
            Text(retryLabel)
        }
    }
}
