package com.bandu.tiji.data.profile

interface LearningDataResetCoordinator {
    suspend fun clearLearningData()

    suspend fun factoryReset()
}
