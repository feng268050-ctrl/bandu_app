package com.bandu.tiji.transfer.protocol.packageio

import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.PortablePreferencesRecord
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.protocol.proto.RecordCounts
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TransferPackageIntegrityTest {
    @Test
    fun `record stream round trips with counts and sha256 validation`() {
        val records = records()
        val output = ByteArrayOutputStream()
        val written = PortableRecordStream.write(records, output)
        val decoded = mutableListOf<PortableRecord>()

        val verified = PortableRecordStream.verify(
            input = ByteArrayInputStream(output.toByteArray()),
            expectedSha256 = written.sha256,
            expectedCounts = written.counts,
            onRecord = decoded::add,
        )

        assertThat(decoded).containsExactlyElementsIn(records).inOrder()
        assertThat(verified.recordCount).isEqualTo(2)
        assertThat(verified.counts.collections).isEqualTo(1)
        assertThat(verified.counts.portablePreferences).isEqualTo(1)
    }

    @Test
    fun `record stream rejects tampering truncation and count mismatch`() {
        val output = ByteArrayOutputStream()
        val written = PortableRecordStream.write(records(), output)
        val encoded = output.toByteArray()

        val tampered = encoded.copyOf().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        assertThrows(PackageIntegrityException::class.java) {
            PortableRecordStream.verify(
                ByteArrayInputStream(tampered),
                written.sha256,
                written.counts,
            )
        }
        assertThrows(PackageIntegrityException::class.java) {
            PortableRecordStream.verify(
                ByteArrayInputStream(encoded.copyOf(encoded.size - 1)),
                written.sha256,
                written.counts,
            )
        }
        assertThrows(PackageIntegrityException::class.java) {
            PortableRecordStream.verify(
                ByteArrayInputStream(encoded),
                written.sha256,
                written.counts.toBuilder().setCollections(2).build(),
            )
        }
    }

    @Test
    fun `manifest verifies every declared file and changes hash with content`() {
        val recordsOutput = ByteArrayOutputStream()
        val recordsSummary = PortableRecordStream.write(records(), recordsOutput)
        val image = "image-bytes".encodeToByteArray()
        val entry = TransferManifestIntegrity.createFileEntry(
            "images/a.jpg",
            ByteArrayInputStream(image),
        )
        val manifest = TransferManifestIntegrity.createManifest(
            schemaVersion = 1,
            exportId = "export-a",
            files = listOf(entry),
            counts = recordsSummary.counts,
            recordsSha256 = recordsSummary.sha256,
        )

        TransferManifestIntegrity.verifyFiles(
            manifest,
            mapOf("images/a.jpg" to { ByteArrayInputStream(image) }),
        )

        val changedManifest = manifest.toBuilder().setExportId("export-b").build()
        assertThat(TransferManifestIntegrity.manifestSha256(manifest))
            .isNotEqualTo(TransferManifestIntegrity.manifestSha256(changedManifest))
    }

    @Test
    fun `missing file and hash mismatch fail package verification`() {
        val image = "image-bytes".encodeToByteArray()
        val entry = TransferManifestIntegrity.createFileEntry(
            "images/a.jpg",
            ByteArrayInputStream(image),
        )
        val manifest = TransferManifestIntegrity.createManifest(
            schemaVersion = 1,
            exportId = "export",
            files = listOf(entry),
            counts = RecordCounts.getDefaultInstance(),
            recordsSha256 = ByteArray(32),
        )

        assertThrows(PackageIntegrityException::class.java) {
            TransferManifestIntegrity.verifyFiles(manifest, emptyMap())
        }
        assertThrows(PackageIntegrityException::class.java) {
            TransferManifestIntegrity.verifyFiles(
                manifest,
                mapOf("images/a.jpg" to { ByteArrayInputStream("wrong".encodeToByteArray()) }),
            )
        }
    }

    @Test
    fun `unsafe and duplicate manifest paths are rejected`() {
        val image = byteArrayOf(1)
        assertThrows(PackageIntegrityException::class.java) {
            TransferManifestIntegrity.createFileEntry(
                "../device/preferences.pb",
                ByteArrayInputStream(image),
            )
        }
        val entry = TransferManifestIntegrity.createFileEntry(
            "images/a.jpg",
            ByteArrayInputStream(image),
        )
        assertThrows(PackageIntegrityException::class.java) {
            TransferManifestIntegrity.createManifest(
                schemaVersion = 1,
                exportId = "export",
                files = listOf(entry, entry),
                counts = RecordCounts.getDefaultInstance(),
                recordsSha256 = ByteArray(32),
            )
        }
    }

    private fun records(): List<PortableRecord> = listOf(
        PortableRecord.newBuilder()
            .setCollection(
                CollectionRecord.newBuilder()
                    .setId("collection")
                    .setName("数学"),
            )
            .build(),
        PortableRecord.newBuilder()
            .setPortablePreferences(
                PortablePreferencesRecord.newBuilder()
                    .setStudentNickname("小明"),
            )
            .build(),
    )
}
