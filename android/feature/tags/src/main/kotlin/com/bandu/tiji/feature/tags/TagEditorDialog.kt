package com.bandu.tiji.feature.tags

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun TagEditorDialog(
    state: TagEditorState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (state.mode) {
                    is TagEditorMode.Create -> "新建自定义标签"
                    is TagEditorMode.Rename -> "修改标签名称"
                },
            )
        },
        text = {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("标签名称") },
                supportingText = state.errorMessage?.let { message ->
                    { Text(message) }
                },
                isError = state.errorMessage != null,
                enabled = !state.isSubmitting,
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSubmitting,
            ) {
                Text(if (state.isSubmitting) "保存中" else "保存")
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
