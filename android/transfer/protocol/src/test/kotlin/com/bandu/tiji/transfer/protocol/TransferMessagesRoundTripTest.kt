package com.bandu.tiji.transfer.protocol

import com.bandu.tiji.transfer.protocol.proto.ChunkAck
import com.bandu.tiji.transfer.protocol.proto.ChunkAckStatus
import com.bandu.tiji.transfer.protocol.proto.ChunkRequest
import com.bandu.tiji.transfer.protocol.proto.CommitDecision
import com.bandu.tiji.transfer.protocol.proto.CommitReady
import com.bandu.tiji.transfer.protocol.proto.DataChunk
import com.bandu.tiji.transfer.protocol.proto.FileEntry
import com.bandu.tiji.transfer.protocol.proto.Hello
import com.bandu.tiji.transfer.protocol.proto.IdentityConfirmation
import com.bandu.tiji.transfer.protocol.proto.IdentityExchange
import com.bandu.tiji.transfer.protocol.proto.Manifest
import com.bandu.tiji.transfer.protocol.proto.PairingStart
import com.bandu.tiji.transfer.protocol.proto.ProtocolError
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.bandu.tiji.transfer.protocol.proto.RecordCounts
import com.bandu.tiji.transfer.protocol.proto.SrpChallenge
import com.bandu.tiji.transfer.protocol.proto.SrpProof
import com.bandu.tiji.transfer.protocol.proto.SrpServerProof
import com.bandu.tiji.transfer.protocol.proto.TransferComplete
import com.bandu.tiji.transfer.protocol.proto.TransferDecision
import com.bandu.tiji.transfer.protocol.proto.TransferOffer
import com.bandu.tiji.transfer.protocol.proto.VerifyResult
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import org.junit.Test

class TransferMessagesRoundTripTest {
    @Test
    fun `all control messages round trip`() {
        val hash = ByteString.copyFrom(ByteArray(32) { it.toByte() })
        val messages = listOf(
            Hello.newBuilder().setDiscoveryId("source").setDeviceName("Phone").setNonce(hash).build(),
            PairingStart.newBuilder().setPairingSessionId("pair").setIdentity("a|b").setClientPublicValue(hash).build(),
            SrpChallenge.newBuilder().setSalt(hash).setServerPublicValue(hash).setExpiresAtEpochMs(10).build(),
            SrpProof.newBuilder().setClientEvidence(hash).build(),
            SrpServerProof.newBuilder().setServerEvidence(hash).build(),
            IdentityExchange.newBuilder()
                .setDeviceId("device")
                .setDisplayName("Phone")
                .setSigningPublicKey(hash)
                .setProofSignature(hash)
                .build(),
            IdentityConfirmation.newBuilder().setAccepted(true).setFingerprint("001122").build(),
            TransferOffer.newBuilder()
                .setSessionId("session")
                .setSourceDeviceId("device")
                .setCreatedAtEpochMs(1)
                .setTotalBytes(2)
                .setTotalFiles(3)
                .setTotalRecords(4)
                .setManifestSha256(hash)
                .build(),
            TransferDecision.newBuilder().setSessionId("session").setAccepted(true).build(),
            Manifest.newBuilder()
                .setSchemaVersion(1)
                .setExportId("export")
                .addFiles(
                    FileEntry.newBuilder()
                        .setRelativePath("images/a.jpg")
                        .setSize(2)
                        .setSha256(hash)
                        .setChunkSize(1)
                        .setChunkCount(2),
                )
                .setCounts(RecordCounts.newBuilder().setErrorItems(1))
                .setRecordsSha256(hash)
                .build(),
            ChunkRequest.newBuilder().setSessionId("session").setRelativePath("images/a.jpg").addMissingIndexes(1).build(),
            DataChunk.newBuilder().setRelativePath("images/a.jpg").setIndex(1).setData(hash).setSha256(hash).build(),
            ChunkAck.newBuilder()
                .setRelativePath("images/a.jpg")
                .setIndex(1)
                .setStatus(ChunkAckStatus.CHUNK_ACK_STATUS_ACCEPTED)
                .build(),
            VerifyResult.newBuilder().setSessionId("session").setValid(false).addFailureCodes("HASH").build(),
            CommitReady.newBuilder().setSessionId("session").setVerifiedManifestSha256(hash).build(),
            CommitDecision.newBuilder().setSessionId("session").setAccepted(true).build(),
            TransferComplete.newBuilder().setSessionId("session").setCompletedAtEpochMs(20).build(),
            ProtocolError.newBuilder()
                .setCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED)
                .setMessage("hash mismatch")
                .setResumable(true)
                .build(),
        )

        messages.forEach(::assertRoundTrip)
    }

    private fun assertRoundTrip(message: MessageLite) {
        @Suppress("UNCHECKED_CAST")
        val parser = message.parserForType as Parser<MessageLite>
        assertThat(parser.parseFrom(message.toByteArray())).isEqualTo(message)
    }
}
