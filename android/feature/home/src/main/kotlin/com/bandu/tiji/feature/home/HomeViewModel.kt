package com.bandu.tiji.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var profileJob: Job? = null

    init {
        loadProfile()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RetryProfile -> loadProfile()
            is HomeAction.OpenDestination -> {
                mutableEffects.trySend(HomeEffect.Navigate(action.intent))
            }
        }
    }

    private fun loadProfile() {
        profileJob?.cancel()
        mutableUiState.value = HomeUiState()
        profileJob = viewModelScope.launch {
            profileRepository.observeProfile()
                .catch {
                    mutableUiState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = PROFILE_LOAD_ERROR,
                    )
                }
                .collect { profile ->
                    mutableUiState.value = HomeUiState(
                        nickname = profile.nickname.trim().ifEmpty { null },
                        isLoading = false,
                    )
                }
        }
    }

    private companion object {
        const val PROFILE_LOAD_ERROR = "无法加载学生资料"
    }
}
