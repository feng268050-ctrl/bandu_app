package com.bandu.tiji

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.navigation.CaptureDestination
import com.bandu.tiji.navigation.DevicesDestination
import com.bandu.tiji.navigation.HomeDestination
import com.bandu.tiji.navigation.ProfileDestination
import com.bandu.tiji.navigation.TopLevelDestination
import com.bandu.tiji.navigation.TopLevelNavigationBar
import com.bandu.tiji.navigation.TutorSessionsDestination

@Composable
fun BanduTijiApp(
    uiState: AppUiState = AppUiState(),
) {
    BanduTijiTheme {
        val navController = rememberNavController()
        val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val selectedDestination = when {
            currentDestination?.hasRoute<HomeDestination>() == true -> HomeDestination
            currentDestination?.hasRoute<DevicesDestination>() == true -> DevicesDestination
            currentDestination?.hasRoute<CaptureDestination>() == true -> CaptureDestination
            currentDestination?.hasRoute<TutorSessionsDestination>() == true -> TutorSessionsDestination
            currentDestination?.hasRoute<ProfileDestination>() == true -> ProfileDestination
            else -> null
        }

        LaunchedEffect(uiState.unhandledError) {
            uiState.unhandledError?.let { error ->
                snackbarHostState.showSnackbar(error)
            }
        }

        Scaffold(
            topBar = {
                uiState.migration?.let { migration ->
                    MigrationStatusBar(migration)
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            bottomBar = {
                if (selectedDestination != null) {
                    TopLevelNavigationBar(
                        selectedDestination = selectedDestination,
                        onNavigate = { destination ->
                            navController.navigate(destination) {
                                popUpTo(HomeDestination) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeDestination,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<HomeDestination> {
                    PlaceholderScreen(title = "首页")
                }
                composable<DevicesDestination> {
                    PlaceholderScreen(title = "设备")
                }
                composable<CaptureDestination> {
                    PlaceholderScreen(title = "新增")
                }
                composable<TutorSessionsDestination> {
                    PlaceholderScreen(title = "AI辅导")
                }
                composable<ProfileDestination> {
                    PlaceholderScreen(title = "我的")
                }
            }
        }
    }
}

data class AppUiState(
    val unhandledError: String? = null,
    val migration: MigrationUiState? = null,
)

data class MigrationUiState(
    val progress: Float,
    val statusText: String,
) {
    init {
        require(progress in 0f..1f) { "progress must be between 0 and 1" }
        require(statusText.isNotBlank()) { "statusText must not be blank" }
    }
}

@Composable
private fun MigrationStatusBar(state: MigrationUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = state.statusText,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "$title - ${stringResource(R.string.app_name)}")
        }
    }
}
