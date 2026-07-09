package com.bandu.tiji

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.DeviceTransferRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.ExerciseRepository
import com.bandu.tiji.domain.repository.ProfileRepository
import com.bandu.tiji.domain.repository.QuestionBankRepository
import com.bandu.tiji.domain.repository.StatsRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.feature.capture.CaptureEffect
import com.bandu.tiji.feature.capture.CaptureFlowScreen
import com.bandu.tiji.feature.capture.CaptureViewModel
import com.bandu.tiji.feature.capture.PhotoPickerLauncher
import com.bandu.tiji.feature.devices.DevicesRoute
import com.bandu.tiji.feature.devices.DevicesViewModel
import com.bandu.tiji.feature.home.HomeRoute
import com.bandu.tiji.feature.home.HomeViewModel
import com.bandu.tiji.feature.library.CollectionListRoute
import com.bandu.tiji.feature.library.CollectionListViewModel
import com.bandu.tiji.feature.library.ErrorItemDetailRoute
import com.bandu.tiji.feature.library.ErrorItemDetailViewModel
import com.bandu.tiji.feature.library.ErrorItemListRoute
import com.bandu.tiji.feature.library.ErrorItemListViewModel
import com.bandu.tiji.feature.profile.ProfileRoute
import com.bandu.tiji.feature.profile.ProfileAction
import com.bandu.tiji.feature.profile.AboutInfo
import com.bandu.tiji.feature.profile.ProfileSection
import com.bandu.tiji.feature.profile.ProfileViewModel
import com.bandu.tiji.feature.questionbank.QuestionBankRoute
import com.bandu.tiji.feature.questionbank.QuestionBankViewModel
import com.bandu.tiji.feature.stats.StatsRoute
import com.bandu.tiji.feature.stats.StatsViewModel
import com.bandu.tiji.feature.tags.TagsRoute
import com.bandu.tiji.feature.tags.TagsViewModel
import com.bandu.tiji.feature.tutor.TutorSessionRoute
import com.bandu.tiji.feature.tutor.TutorSessionViewModel
import com.bandu.tiji.feature.tutor.TutorSessionsRoute
import com.bandu.tiji.feature.tutor.TutorSessionsViewModel
import com.bandu.tiji.navigation.AiSettingsDestination
import com.bandu.tiji.navigation.CaptureDestination
import com.bandu.tiji.navigation.CollectionDestination
import com.bandu.tiji.navigation.DevicesDestination
import com.bandu.tiji.navigation.ErrorItemDetailDestination
import com.bandu.tiji.navigation.HomeDestination
import com.bandu.tiji.navigation.LibraryDestination
import com.bandu.tiji.navigation.ProfileDestination
import com.bandu.tiji.navigation.QuestionBanksDestination
import com.bandu.tiji.navigation.StatsDestination
import com.bandu.tiji.navigation.TagsDestination
import com.bandu.tiji.navigation.TopLevelDestination
import com.bandu.tiji.navigation.TopLevelNavigationBar
import com.bandu.tiji.navigation.TutorSessionDestination
import com.bandu.tiji.navigation.TutorSessionsDestination
import com.bandu.tiji.navigation.toAppDestination
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun BanduTijiApp(
    uiState: AppUiState = AppUiState(),
) {
    BanduTijiTheme {
        val context = LocalContext.current.applicationContext
        val dependencies = remember(context) {
            EntryPointAccessors.fromApplication(
                context,
                AppDependencies::class.java,
            )
        }
        val navController = rememberNavController()
        val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
        val profileViewModel = appViewModel("profile") {
            val defaultDeviceName = android.os.Build.MODEL ?: "Android"
            ProfileViewModel(
                profileRepository = dependencies.profileRepository(),
                deviceNameStore = DevicePreferencesDeviceNameStore(
                    preferences = dependencies.devicePreferencesStore(),
                    fallbackName = defaultDeviceName,
                ),
                avatarStore = PortableProfileAvatarStore(
                    preferences = dependencies.portablePreferencesStore(),
                ),
                aiConfigurationRepository = dependencies.aiConfigurationRepository(),
                remoteAdbDebugController = AndroidRemoteAdbDebugController(context),
                aboutInfo = AboutInfo(versionName = BuildConfig.VERSION_NAME),
            )
        }
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
                            if (destination == ProfileDestination) {
                                profileViewModel.onAction(ProfileAction.Back)
                            }
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
                    val viewModel = appViewModel("home") {
                        HomeViewModel(dependencies.profileRepository())
                    }
                    HomeRoute(
                        viewModel = viewModel,
                        onNavigate = { intent ->
                            navController.navigate(intent.toAppDestination())
                        },
                    )
                }
                composable<DevicesDestination> {
                    val viewModel = appViewModel("devices") {
                        DevicesViewModel(
                            repository = dependencies.deviceTransferRepository(),
                            localDeviceName = android.os.Build.MODEL ?: "Android",
                            localFingerprint = "本机",
                        )
                    }
                    DevicesRoute(viewModel)
                }
                composable<CaptureDestination> {
                    val viewModel = appViewModel("capture") {
                        CaptureViewModel(
                            aiGateway = dependencies.aiTutorGateway(),
                            errorItemRepository = dependencies.errorItemRepository(),
                        )
                    }
                    CaptureRoute(
                        viewModel = viewModel,
                        onNavigate = { intent ->
                            navController.navigate(intent.toAppDestination())
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<TutorSessionsDestination> {
                    val viewModel = appViewModel("tutor-sessions") {
                        TutorSessionsViewModel(dependencies.tutorRepository())
                    }
                    TutorSessionsRoute(
                        viewModel = viewModel,
                        onOpenSession = { sessionId ->
                            navController.navigate(
                                TutorSessionDestination(
                                    sessionId = sessionId.value,
                                    errorItemId = null,
                                ),
                            )
                        },
                    )
                }
                composable<ProfileDestination> {
                    ProfileRouteWithAvatarPicker(profileViewModel)
                }
                composable<LibraryDestination> {
                    val viewModel = appViewModel("library") {
                        CollectionListViewModel(dependencies.collectionRepository())
                    }
                    CollectionListRoute(
                        viewModel = viewModel,
                        onNavigate = { intent ->
                            navController.navigate(intent.toAppDestination())
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<CollectionDestination> { entry ->
                    val route = entry.toRoute<CollectionDestination>()
                    val viewModel = appViewModel("collection-${route.collectionId}") {
                        ErrorItemListViewModel(
                            repository = dependencies.errorItemRepository(),
                            collectionRepository = dependencies.collectionRepository(),
                            tagRepository = dependencies.tagRepository(),
                            initialQuery = ErrorItemQuery(
                                collectionId = CollectionId(route.collectionId),
                            ),
                        )
                    }
                    ErrorItemListRoute(
                        viewModel = viewModel,
                        onNavigate = { intent ->
                            navController.navigate(intent.toAppDestination())
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<TagsDestination> {
                    val viewModel = appViewModel("tags") {
                        TagsViewModel(dependencies.tagRepository())
                    }
                    TagsRoute(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<StatsDestination> {
                    val viewModel = appViewModel("stats") {
                        StatsViewModel(dependencies.statsRepository())
                    }
                    StatsRoute(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<QuestionBanksDestination> {
                    val viewModel = appViewModel("question-banks") {
                        QuestionBankViewModel(dependencies.questionBankRepository())
                    }
                    QuestionBankRoute(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<ErrorItemDetailDestination> { entry ->
                    val route = entry.toRoute<ErrorItemDetailDestination>()
                    val viewModel = appViewModel("error-item-${route.errorItemId}") {
                        ErrorItemDetailViewModel(
                            repository = dependencies.errorItemRepository(),
                            errorItemId = ErrorItemId(route.errorItemId),
                            collectionRepository = dependencies.collectionRepository(),
                            tagRepository = dependencies.tagRepository(),
                        )
                    }
                    ErrorItemDetailRoute(
                        viewModel = viewModel,
                        onNavigate = { intent ->
                            navController.navigate(intent.toAppDestination())
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<TutorSessionDestination> { entry ->
                    val route = entry.toRoute<TutorSessionDestination>()
                    val sessionId = route.sessionId
                    if (sessionId == null) {
                        val viewModel = appViewModel("tutor-sessions") {
                            TutorSessionsViewModel(dependencies.tutorRepository())
                        }
                        TutorSessionsRoute(
                            viewModel = viewModel,
                            onOpenSession = { id ->
                                navController.navigate(
                                    TutorSessionDestination(
                                        sessionId = id.value,
                                        errorItemId = route.errorItemId,
                                    ),
                                )
                            },
                        )
                    } else {
                        val viewModel = appViewModel("tutor-session-$sessionId") {
                            TutorSessionViewModel(
                                tutorRepository = dependencies.tutorRepository(),
                                sessionId = TutorSessionId(sessionId),
                                aiTutorGateway = dependencies.aiTutorGateway(),
                                exerciseRepository = dependencies.exerciseRepository(),
                            )
                        }
                        TutorSessionRoute(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenAiConfiguration = {
                                navController.navigate(AiSettingsDestination)
                            },
                        )
                    }
                }
                composable<AiSettingsDestination> {
                    LaunchedEffect(profileViewModel) {
                        profileViewModel.onAction(
                            ProfileAction.OpenSection(ProfileSection.AI),
                        )
                    }
                    ProfileRouteWithAvatarPicker(profileViewModel)
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
private fun CaptureRoute(
    viewModel: CaptureViewModel,
    onNavigate: (com.bandu.tiji.core.model.navigation.NavigationIntent) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPhotoPicker by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CaptureEffect.LaunchPhotoPicker -> showPhotoPicker = true
                CaptureEffect.NavigateBack -> onBack()
                is CaptureEffect.Navigate -> onNavigate(effect.intent)
            }
        }
    }
    CaptureFlowScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
    if (showPhotoPicker) {
        PhotoPickerLauncher(
            onPicked = { uri ->
                showPhotoPicker = false
                viewModel.onAction(com.bandu.tiji.feature.capture.CaptureAction.ImageSelected(uri))
            },
            onCancelled = {
                showPhotoPicker = false
            },
        )
    }
}

@Composable
private fun ProfileRouteWithAvatarPicker(
    viewModel: ProfileViewModel,
) {
    var showAvatarPicker by remember { mutableStateOf(false) }
    ProfileRoute(
        viewModel = viewModel,
        onAvatarImagePickerRequested = { showAvatarPicker = true },
    )
    if (showAvatarPicker) {
        PhotoPickerLauncher(
            onPicked = { uri ->
                showAvatarPicker = false
                viewModel.onAction(ProfileAction.UpdateAvatarImage(uri))
            },
            onCancelled = {
                showAvatarPicker = false
            },
        )
    }
}

@Composable
private inline fun <reified VM : ViewModel> appViewModel(
    key: String,
    crossinline create: () -> VM,
): VM {
    val factory = remember(key) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = create() as T
        }
    }
    return viewModel(key = key, factory = factory)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppDependencies {
    fun portablePreferencesStore(): PortablePreferencesStore
    fun devicePreferencesStore(): DevicePreferencesStore
    fun profileRepository(): ProfileRepository
    fun questionBankRepository(): QuestionBankRepository
    fun collectionRepository(): CollectionRepository
    fun errorItemRepository(): ErrorItemRepository
    fun tagRepository(): TagRepository
    fun statsRepository(): StatsRepository
    fun tutorRepository(): TutorRepository
    fun exerciseRepository(): ExerciseRepository
    fun aiConfigurationRepository(): AiConfigurationRepository
    fun aiTutorGateway(): AiTutorGateway
    fun deviceTransferRepository(): DeviceTransferRepository
}
