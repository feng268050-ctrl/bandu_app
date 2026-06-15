package com.bandu.tiji.transfer.protocol

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.nimbusds.srp6.SRP6CryptoParams
import org.junit.Test

class ProtocolModuleTest {
    @Test
    fun `module exposes protobuf lite and Nimbus SRP`() {
        assertThat(ByteString.copyFromUtf8("bandu").toStringUtf8()).isEqualTo("bandu")
        assertThat(SRP6CryptoParams.getInstance(2048, "SHA-256")).isNotNull()
    }
}
