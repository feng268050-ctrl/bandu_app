package com.bandu.tiji.feature.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun TagDeleteDialog(
    state: TagDeleteConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除自定义标签") },
        text = {
            Text(
                text = "“${state.name}”关联 ${state.linkedErrorItemCount} 道错题。" +
                    "删除标签不会删除这些错题。",
                color = if (state.errorMessage == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            state.errorMessage?.let { Text(it) }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSubmitting,
            ) {
                Text(if (state.isSubmitting) "删除中" else "确认删除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSubmitting,
            ) {
                Text("取消")
            }
        },
    )
}
