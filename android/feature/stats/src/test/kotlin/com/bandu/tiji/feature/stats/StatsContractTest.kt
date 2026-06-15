package com.bandu.tiji.feature.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatsContractTest {
    @Test
    fun `initial state contains empty wrong item and exercise sections`() {
        val state = StatsUiState()

        assertThat(state.isLoading).isTrue()
        assertThat(state.errorMessage).isNull()
        assertThat(state.wrongItemStats.totalCount).isEqualTo(0)
        assertThat(state.exerciseStats.totalCount).isEqualTo(0)
    }
}
