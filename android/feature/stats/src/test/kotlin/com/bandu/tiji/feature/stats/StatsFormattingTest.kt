package com.bandu.tiji.feature.stats

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatsFormattingTest {
    @Test
    fun `percentage formatting is bounded and stable`() {
        assertThat(formatPercentage(0.0)).isEqualTo("0%")
        assertThat(formatPercentage(0.625)).isEqualTo("62.5%")
        assertThat(formatPercentage(1.0)).isEqualTo("100%")
        assertThat(formatPercentage(1.5)).isEqualTo("100%")
    }

    @Test
    fun `exercise difficulty labels are stable simplified Chinese`() {
        assertThat(ExerciseDifficulty.entries.map(::difficultyLabel)).containsExactly(
            "简单",
            "中等",
            "困难",
            "挑战",
        ).inOrder()
    }
}
