package com.bandu.tiji.feature.stats

import com.bandu.tiji.core.model.stats.MonthlyCount
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MonthlyStatsTest {
    @Test
    fun `last six months fills missing entries across year boundary`() {
        val result = fillLastSixMonths(
            listOf(
                MonthlyCount(2025, 11, 2),
                MonthlyCount(2026, 1, 3),
                MonthlyCount(2026, 4, 5),
            ),
        )

        assertThat(result).containsExactly(
            MonthlyCount(2025, 11, 2),
            MonthlyCount(2025, 12, 0),
            MonthlyCount(2026, 1, 3),
            MonthlyCount(2026, 2, 0),
            MonthlyCount(2026, 3, 0),
            MonthlyCount(2026, 4, 5),
        ).inOrder()
    }

    @Test
    fun `last six months aggregates duplicate months and ignores older entries`() {
        val result = fillLastSixMonths(
            listOf(
                MonthlyCount(2025, 10, 99),
                MonthlyCount(2026, 1, 2),
                MonthlyCount(2026, 1, 3),
                MonthlyCount(2026, 6, 1),
            ),
        )

        assertThat(result.first()).isEqualTo(MonthlyCount(2026, 1, 5))
        assertThat(result.last()).isEqualTo(MonthlyCount(2026, 6, 1))
        assertThat(result).hasSize(6)
    }

    @Test
    fun `empty monthly data remains empty`() {
        assertThat(fillLastSixMonths(emptyList())).isEmpty()
    }
}
