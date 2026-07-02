package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteAdbDebugViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remote adb enable success stores connection state and expiry`() = runTest {
        val controller = RecordingRemoteAdbDebugController(
            initial = RemoteAdbDebugRuntimeState(
                isSupported = true,
                isEnabled = false,
                ipAddress = "192.168.1.20",
            ),
        )
        val clock = TestClock(initialEpochMillis = 1_000L)
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            clock = clock,
            remoteAdbDebugController = controller,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.RequestEnableRemoteAdbDebug)
        assertThat(viewModel.uiState.value.remoteAdbDebug.showEnableConfirmation).isTrue()

        controller.nextEnableState = RemoteAdbDebugRuntimeState(
            isSupported = true,
            isEnabled = true,
            port = DEFAULT_REMOTE_ADB_PORT,
            ipAddress = "192.168.1.20",
        )
        viewModel.onAction(ProfileAction.ConfirmEnableRemoteAdbDebug)
        runCurrent()

        val state = viewModel.uiState.value.remoteAdbDebug
        assertThat(controller.enablePorts).containsExactly(DEFAULT_REMOTE_ADB_PORT)
        assertThat(state.isEnabled).isTrue()
        assertThat(state.connectCommand).isEqualTo("adb connect 192.168.1.20:5555")
        assertThat(state.expiresAtEpochMillis).isEqualTo(
            1_000L + ProfileViewModel.REMOTE_ADB_TTL_MILLIS,
        )
        assertThat(state.statusMessage).isEqualTo("ADB 远程调试已开启")
    }

    @Test
    fun `remote adb disable clears expiry`() = runTest {
        val controller = RecordingRemoteAdbDebugController(
            initial = RemoteAdbDebugRuntimeState(
                isSupported = true,
                isEnabled = true,
                port = DEFAULT_REMOTE_ADB_PORT,
                ipAddress = "192.168.1.20",
            ),
        )
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            remoteAdbDebugController = controller,
        )
        advanceUntilIdle()

        controller.nextDisableState = RemoteAdbDebugRuntimeState(
            isSupported = true,
            isEnabled = false,
            port = DEFAULT_REMOTE_ADB_PORT,
            ipAddress = "192.168.1.20",
        )
        viewModel.onAction(ProfileAction.DisableRemoteAdbDebug)
        runCurrent()

        val state = viewModel.uiState.value.remoteAdbDebug
        assertThat(controller.disableCalls).isEqualTo(1)
        assertThat(state.isEnabled).isFalse()
        assertThat(state.expiresAtEpochMillis).isNull()
        assertThat(state.statusMessage).isEqualTo("ADB 远程调试已关闭")
    }

    @Test
    fun `opening about refreshes remote adb state`() = runTest {
        val controller = RecordingRemoteAdbDebugController(
            initial = RemoteAdbDebugRuntimeState(
                isSupported = true,
                isEnabled = false,
                ipAddress = "192.168.1.20",
            ),
        )
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            remoteAdbDebugController = controller,
        )
        advanceUntilIdle()

        controller.nextRefreshState = RemoteAdbDebugRuntimeState(
            isSupported = true,
            isEnabled = true,
            port = DEFAULT_REMOTE_ADB_PORT,
            ipAddress = "192.168.1.20",
        )
        viewModel.onAction(ProfileAction.OpenSection(ProfileSection.ABOUT))
        runCurrent()

        val state = viewModel.uiState.value.remoteAdbDebug
        assertThat(controller.refreshCalls).isEqualTo(1)
        assertThat(state.isEnabled).isTrue()
        assertThat(state.errorMessage).isNull()
        assertThat(state.connectCommand).isEqualTo("adb connect 192.168.1.20:5555")
    }
}

private class RecordingRemoteAdbDebugController(
    initial: RemoteAdbDebugRuntimeState,
) : RemoteAdbDebugController {
    private val state = MutableStateFlow(initial)
    var nextEnableState: RemoteAdbDebugRuntimeState = initial.copy(isEnabled = true)
    var nextDisableState: RemoteAdbDebugRuntimeState = initial.copy(isEnabled = false)
    var nextRefreshState: RemoteAdbDebugRuntimeState = initial
    val enablePorts = mutableListOf<Int>()
    var disableCalls = 0
    var refreshCalls = 0

    override fun observeState() = state

    override suspend fun refresh(): RemoteAdbDebugRuntimeState {
        refreshCalls += 1
        state.value = nextRefreshState
        return nextRefreshState
    }

    override suspend fun enable(port: Int): RemoteAdbDebugOperationResult {
        enablePorts += port
        state.value = nextEnableState
        return RemoteAdbDebugOperationResult.Success(nextEnableState)
    }

    override suspend fun disable(): RemoteAdbDebugOperationResult {
        disableCalls += 1
        state.value = nextDisableState
        return RemoteAdbDebugOperationResult.Success(nextDisableState)
    }
}
