package com.bandu.tiji.core.common.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppResultTest {
    @Test
    fun success_mapsValue() {
        val result = AppResult.Success(2).map { it * 3 }

        assertThat(result).isEqualTo(AppResult.Success(6))
    }

    @Test
    fun failure_preservesErrorOnMap() {
        val error = AppError.Validation("empty_name")
        val result = AppResult.Failure(error).map { it }

        assertThat(result).isEqualTo(AppResult.Failure(error))
    }

    @Test
    fun catching_wrapsUnexpectedErrors() {
        val result = AppResult.catching<String> { error("boom") }

        assertThat(result.isSuccess).isFalse()
        val failure = result as AppResult.Failure
        assertThat(failure.error).isInstanceOf(AppError.Unexpected::class.java)
    }
}
