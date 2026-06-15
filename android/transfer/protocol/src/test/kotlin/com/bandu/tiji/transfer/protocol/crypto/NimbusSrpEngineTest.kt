package com.bandu.tiji.transfer.protocol.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.security.MessageDigest

class NimbusSrpEngineTest {
    @Test
    fun `uses fixed RFC 5054 2048 bit group and SHA-256`() {
        assertThat(NimbusSrpEngine.PARAMS.N.bitLength()).isEqualTo(2048)
        assertThat(NimbusSrpEngine.PARAMS.g.toInt()).isEqualTo(2)
        assertThat(NimbusSrpEngine.PARAMS.H).isEqualTo("SHA-256")
    }

    @Test
    fun `RFC identity password formula matches fixed SHA-256 vector`() {
        val salt = "BEB25379D1A8581EB5A727673A2441EE".hexToByteArray()
        val actual = NimbusSrpEngine.RFC_X_ROUTINE.computeX(
            MessageDigest.getInstance("SHA-256"),
            salt,
            "alice".encodeToByteArray(),
            "password123".encodeToByteArray(),
        )

        assertThat(actual.toString(16).padStart(64, '0'))
            .isEqualTo("0065ac38dff8bc34ae0f259e91fbd0f4ca2fa43081c9050cec7cac20d015f303")
    }

    @Test
    fun `client and server derive the same shared secret`() {
        val engine = NimbusSrpEngine()
        val code = "012345".toCharArray()
        val identity = "device-a|device-b".encodeToByteArray()
        val client = engine.createClient(code, identity)
        val server = engine.createServer(code, identity)

        val challenge = server.challenge(client.start())
        val clientProof = client.answer(challenge)
        val serverResult = server.verify(clientProof)
        val clientSecret = client.verify(serverResult.serverEvidence)

        assertThat(clientSecret).isEqualTo(serverResult.sharedSecret)
        assertThat(clientSecret).isNotEmpty()
    }
}
