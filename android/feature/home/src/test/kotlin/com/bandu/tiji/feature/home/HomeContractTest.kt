package com.bandu.tiji.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeContractTest {
    @Test
    fun `initial state is loading without stale profile data`() {
        assertThat(HomeUiState()).isEqualTo(
            HomeUiState(
                nickname = null,
                isLoading = true,
                errorMessage = null,
            ),
        )
    }
}
