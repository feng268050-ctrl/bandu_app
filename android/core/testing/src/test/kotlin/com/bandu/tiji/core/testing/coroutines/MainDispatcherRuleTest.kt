package com.bandu.tiji.core.testing.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `main dispatcher work is controlled by test scheduler`() {
        var completed = false

        CoroutineScope(Dispatchers.Main).launch {
            completed = true
        }

        assertThat(completed).isFalse()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertThat(completed).isTrue()
    }
}
