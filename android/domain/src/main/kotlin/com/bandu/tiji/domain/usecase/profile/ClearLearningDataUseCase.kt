package com.bandu.tiji.domain.usecase.profile

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.repository.ProfileRepository

/**
 * Clears learning content only: collections, error items, images, tags, stats records,
 * tutor sessions and exercises. Preserves student profile, AI configuration, API keys,
 * device identity and trusted peers.
 */
class ClearLearningDataUseCase(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> =
        runSuspendCatching { repository.clearLearningData() }
}
