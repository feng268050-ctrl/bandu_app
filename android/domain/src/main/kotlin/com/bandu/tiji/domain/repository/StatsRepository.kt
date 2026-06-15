package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.WrongItemStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun observeWrongItemStats(): Flow<WrongItemStats>

    fun observeExerciseStats(): Flow<ExerciseStats>
}
