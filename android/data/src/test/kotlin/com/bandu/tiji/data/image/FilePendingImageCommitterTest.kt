package com.bandu.tiji.data.image

import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.storage.image.ImageOrphanCleanupQueue
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FilePendingImageCommitterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `commit moves pending image and thumbnail into image root`() {
        val filesRoot = temporaryFolder.root
        val pendingDirectory = File(filesRoot, "temp/capture/draft").apply { mkdirs() }
        File(pendingDirectory, "image.jpg").writeText("image")
        File(pendingDirectory, "thumbnail.jpg").writeText("thumbnail")
        val imageRoot = File(filesRoot, "slots/slot_a/images")
        val committer = committer(filesRoot, imageRoot)
        val pending = StoredImage(
            relativePath = "temp/capture/draft/image.jpg",
            sha256Hex = "abc",
            width = 100,
            height = 80,
            thumbnailRelativePath = "temp/capture/draft/thumbnail.jpg",
        )

        val commit = committer.commit(pending, ErrorItemId("error-1"))

        assertThat(commit.image?.relativePath).isEqualTo("images/error-1.jpg")
        assertThat(commit.image?.thumbnailRelativePath).isEqualTo("images/error-1-thumb.jpg")
        assertThat(File(imageRoot, "error-1.jpg").readText()).isEqualTo("image")
        assertThat(File(imageRoot, "error-1-thumb.jpg").readText()).isEqualTo("thumbnail")
        assertThat(File(filesRoot, pending.relativePath).exists()).isFalse()
    }

    @Test
    fun `rollback removes files created by commit`() {
        val filesRoot = temporaryFolder.root
        val source = File(filesRoot, "temp/capture/image.jpg").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        val imageRoot = File(filesRoot, "slots/slot_a/images")
        val committer = committer(filesRoot, imageRoot)
        val commit = committer.commit(
            StoredImage(
                relativePath = filesRoot.toPath().relativize(source.toPath()).toString(),
                sha256Hex = "abc",
                width = 100,
                height = 80,
            ),
            ErrorItemId("error-1"),
        )

        committer.rollback(commit)

        assertThat(File(imageRoot, "error-1.jpg").exists()).isFalse()
        assertThat(ImageOrphanCleanupQueue(imageRoot).pendingRelativePaths()).isEmpty()
    }

    @Test
    fun `commit rejects paths outside temp directory`() {
        val filesRoot = temporaryFolder.root
        val imageRoot = File(filesRoot, "slots/slot_a/images")
        val committer = committer(filesRoot, imageRoot)

        val error = runCatching {
            committer.commit(
                StoredImage("temp/../../outside.jpg", "abc", 100, 80),
                ErrorItemId("error-1"),
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun committer(
        filesRoot: File,
        imageRoot: File,
    ): FilePendingImageCommitter =
        FilePendingImageCommitter(
            filesRoot = filesRoot,
            imageRoot = imageRoot,
            cleanupQueue = ImageOrphanCleanupQueue(imageRoot),
        )
}
