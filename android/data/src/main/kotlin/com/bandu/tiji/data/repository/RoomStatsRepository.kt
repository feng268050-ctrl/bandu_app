package com.bandu.tiji.data.repository

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.data.mapper.toDomain
import com.bandu.tiji.domain.repository.StatsRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class RoomStatsRepository private constructor(
    private val database: LearningDatabase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : StatsRepository {
    @Inject
    constructor(
        database: LearningDatabase,
        clock: Clock,
    ) : this(database, clock, ZoneId.systemDefault())

    private val statsDao = database.statsDao()

    override fun observeWrongItemStats(): Flow<WrongItemStats> =
        combine(
            statsDao.observeWrongItemTotals(),
            statsDao.observeWrongItemSubjectCounts(),
            database.invalidationTracker
                .createFlow("error_items", emitInitialState = true)
                .map { loadWrongItemMonthlyCounts() },
        ) { totals, subjects, monthly ->
            totals.toDomain(subjects, monthly)
        }

    override fun observeExerciseStats(): Flow<ExerciseStats> =
        combine(
            statsDao.observeExerciseTotals(),
            statsDao.observeExerciseSubjectCounts(),
            statsDao.observeExerciseDifficultyCounts(),
            database.invalidationTracker
                .createFlow("exercises", emitInitialState = true)
                .map { loadExerciseCalendarStats() },
        ) { totals, subjects, difficulties, calendar ->
            totals.toDomain(
                subjectCounts = subjects,
                difficultyCounts = difficulties,
                monthlyPracticeCounts = calendar.monthlyCounts,
                activeDaysLastSixMonths = calendar.activeDays,
            )
        }

    private suspend fun loadWrongItemMonthlyCounts(): List<MonthlyCount> =
        recentMonths().map { month ->
            val (start, end) = month.epochRange()
            MonthlyCount(
                year = month.year,
                month = month.monthValue,
                count = statsDao.countWrongItemsCreatedBetween(start, end),
            )
        }

    private suspend fun loadExerciseCalendarStats(): ExerciseCalendarStats {
        val months = recentMonths()
        val monthlyCounts = months.map { month ->
            val (start, end) = month.epochRange()
            MonthlyCount(
                year = month.year,
                month = month.monthValue,
                count = statsDao.countExercisesCreatedBetween(start, end),
            )
        }
        val rangeStart = months.first().atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rangeEnd = months.last().plusMonths(1).atDay(1).atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val offsetSeconds = zoneId.rules
            .getOffset(Instant.ofEpochMilli(clock.nowEpochMillis()))
            .totalSeconds
        val offsetModifier = "%+d seconds".format(offsetSeconds)
        return ExerciseCalendarStats(
            monthlyCounts = monthlyCounts,
            activeDays = statsDao.countActiveExerciseDays(
                startInclusive = rangeStart,
                endExclusive = rangeEnd,
                utcOffsetModifier = offsetModifier,
            ),
        )
    }

    private fun recentMonths(): List<YearMonth> {
        val current = YearMonth.from(
            Instant.ofEpochMilli(clock.nowEpochMillis()).atZone(zoneId),
        )
        return (MONTH_COUNT - 1 downTo 0).map { offset -> current.minusMonths(offset.toLong()) }
    }

    private fun YearMonth.epochRange(): Pair<Long, Long> =
        atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() to
            plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    private data class ExerciseCalendarStats(
        val monthlyCounts: List<MonthlyCount>,
        val activeDays: Int,
    )

    companion object {
        private const val MONTH_COUNT = 6

        internal fun forTest(
            database: LearningDatabase,
            clock: Clock,
            zoneId: ZoneId,
        ): RoomStatsRepository = RoomStatsRepository(database, clock, zoneId)
    }
}
