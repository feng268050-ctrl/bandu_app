package com.bandu.tiji.core.common.coroutines

import kotlinx.coroutines.CancellationException
import java.io.Closeable
import kotlin.coroutines.cancellation.CancellationException as KotlinCancellationException

inline fun <T : Closeable?, R> T.useCancellable(block: (T) -> R): R {
    var thrown: Throwable? = null
    try {
        return block(this)
    } catch (cancelled: CancellationException) {
        thrown = cancelled
        throw cancelled
    } catch (cancelled: KotlinCancellationException) {
        thrown = cancelled
        throw cancelled
    } catch (error: Throwable) {
        thrown = error
        throw error
    } finally {
        if (thrown !is CancellationException && thrown !is KotlinCancellationException) {
            try {
                this?.close()
            } catch (closeError: Throwable) {
                if (thrown == null) throw closeError
            }
        }
    }
}

inline fun rethrowCancellation(block: () -> Unit) {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (cancelled: KotlinCancellationException) {
        throw cancelled
    }
}
