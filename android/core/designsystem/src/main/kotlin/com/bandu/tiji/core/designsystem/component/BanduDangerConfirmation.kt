package com.bandu.tiji.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import com.bandu.tiji.core.designsystem.accessibility.banduMinimumTouchTarget
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun BanduConfirmationTextField(
    expectedText: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("输入“$expectedText”以确认") },
        supportingText = {
            if (value.isNotEmpty() && value != expectedText) {
                Text("确认文本不匹配")
            }
        },
        isError = value.isNotEmpty() && value != expectedText,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}

@Composable
fun BanduDangerConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "确认",
    dismissLabel: String = "取消",
    requiredConfirmationText: String? = null,
    confirmationText: String = "",
    onConfirmationTextChange: (String) -> Unit = {},
) {
    val canConfirm = requiredConfirmationText == null ||
        confirmationText == requiredConfirmationText

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column {
                Text(message)
                if (requiredConfirmationText != null) {
                    BanduConfirmationTextField(
                        expectedText = requiredConfirmationText,
                        value = confirmationText,
                        onValueChange = onConfirmationTextChange,
                        modifier = Modifier.padding(top = BanduSpacing.PageHorizontal),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                modifier = Modifier.banduMinimumTouchTarget(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.banduMinimumTouchTarget(),
            ) {
                Text(dismissLabel)
            }
        },
    )
}
