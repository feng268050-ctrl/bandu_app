package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
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

        assertThat(clientSecret).isEqualTo(serverResult.temporaryKey)
        assertThat(clientSecret).isNotEmpty()
    }

    @Test
    fun `tampered client and server evidence are authentication failures`() {
        val engine = NimbusSrpEngine()
        val identity = "device-a|device-b".encodeToByteArray()
        val client = engine.createClient("012345".toCharArray(), identity)
        val server = engine.createServer("012345".toCharArray(), identity)
        val challenge = server.challenge(client.start())
        val proof = client.answer(challenge)
        proof.clientEvidence[0] = (proof.clientEvidence[0].toInt() xor 1).toByte()

        val clientError = assertThrows(SrpFailureException::class.java) {
            server.verify(proof)
        }
        assertThat(clientError.code).isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)

        val validClient = engine.createClient("012345".toCharArray(), identity)
        val validServer = engine.createServer("012345".toCharArray(), identity)
        val validProof = validClient.answer(validServer.challenge(validClient.start()))
        val serverResult = validServer.verify(validProof)
        serverResult.serverEvidence[0] = (serverResult.serverEvidence[0].toInt() xor 1).toByte()
        val serverError = assertThrows(SrpFailureException::class.java) {
            validClient.verify(serverResult.serverEvidence)
        }
        assertThat(serverError.code).isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)
    }

    @Test
    fun `expired SRP session reports pairing expiration`() {
        val clock = MutableClock(0)
        val client = NimbusSrpEngine(clock = clock)
            .createClient("012345".toCharArray(), "a|b".encodeToByteArray())
        clock.now = 5 * 60 * 1_000L

        val error = assertThrows(SrpFailureException::class.java) {
            client.start()
        }

        assertThat(error.code).isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_PAIRING_CODE_EXPIRED)
    }

    private class MutableClock(var now: Long) : Clock {
        override fun nowEpochMillis(): Long = now
    }
}
