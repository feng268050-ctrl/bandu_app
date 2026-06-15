package com.bandu.tiji.core.common.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

class CoroutineHelpersTest {
    @Test
    fun useCancellable_closesResourceOnSuccess() = runTest {
        val closed = AtomicBoolean(false)
        val resource = Closeable { closed.set(true) }

        val value = resource.useCancellable { 42 }

        assertThat(value).isEqualTo(42)
        assertThat(closed.get()).isTrue()
    }

    @Test(expected = CancellationException::class)
    fun useCancellable_doesNotSwallowCancellation() = runTest {
        val closed = AtomicBoolean(false)
        val resource = Closeable { closed.set(true) }

        resource.useCancellable {
            throw CancellationException("cancelled")
        }
    }
}
