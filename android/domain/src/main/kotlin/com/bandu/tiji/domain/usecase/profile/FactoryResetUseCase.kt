package com.bandu.tiji.domain.usecase.profile

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.ProfileRepository

/**
 * Wipes all local app data including learning content, profile, AI configuration, API keys,
 * storage slots and device preferences, then generates a new device identity.
 */
class FactoryResetUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> =
        runSuspendCatching { repository.factoryReset() }
}
