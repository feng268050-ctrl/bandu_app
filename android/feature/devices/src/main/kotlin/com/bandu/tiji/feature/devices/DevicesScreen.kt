package com.bandu.tiji.feature.devices

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduErrorState
import com.bandu.tiji.core.designsystem.component.BanduLoadingState
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold

@Composable
fun DevicesScreen(
    uiState: DevicesUiState,
    onAction: (DevicesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "设备",
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> BanduLoadingState(
                message = "正在加载设备信息",
                modifier = Modifier,
            )
            uiState.errorMessage != null -> BanduErrorState(
                message = uiState.errorMessage,
                onRetry = { onAction(DevicesAction.Retry) },
            )
            else -> Text("设备迁移", modifier = Modifier)
        }
    }
}
