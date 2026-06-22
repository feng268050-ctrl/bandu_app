package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
internal fun DeviceNameScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "设备名称",
        modifier = modifier,
        navigation = { ProfileBackButton(onAction) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
        ) {
            OutlinedTextField(
                value = uiState.deviceName,
                onValueChange = { onAction(ProfileAction.UpdateDeviceName(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("设备名称") },
                supportingText = {
                    Text(
                        uiState.deviceNameErrorMessage
                            ?: "${uiState.deviceName.length}/${ProfileViewModel.MAX_DEVICE_NAME_LENGTH}",
                    )
                },
                isError = uiState.deviceNameErrorMessage != null,
                singleLine = true,
            )
            Text("此名称会在附近设备列表和配对确认中显示。")
        }
    }
}
