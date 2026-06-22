package com.bandu.tiji.feature.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CaptureContractTest {
    @Test
    fun `capture starts by selecting an image source`() {
        assertThat(CaptureUiState()).isEqualTo(
            CaptureUiState(
                stage = CaptureStage.SelectSource,
                errorMessage = null,
            ),
        )
    }

    @Test
    fun `crop stage accepts quarter turn rotations`() {
        val stages = listOf(0, 90, 180, 270).map { rotation ->
            CaptureStage.Crop(
                draftId = "draft-1",
                tempUri = "content://capture/source",
                rotationDegrees = rotation,
            )
        }

        assertThat(stages.map(CaptureStage.Crop::rotationDegrees))
            .containsExactly(0, 90, 180, 270)
            .inOrder()
    }

    @Test
    fun `invalid progress and rotation are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptureStage.Processing(draftId = "draft-1", progress = 101)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CaptureStage.Crop(
                draftId = "draft-1",
                tempUri = "content://capture/source",
                rotationDegrees = 45,
            )
        }
    }
}
