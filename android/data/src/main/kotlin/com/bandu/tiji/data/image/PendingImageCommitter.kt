package com.bandu.tiji.data.image

import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.storage.image.ImageOrphanCleanupQueue
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface PendingImageCommitter {
    fun commit(
        image: StoredImage?,
        errorItemId: ErrorItemId,
    ): ImageCommit

    fun rollback(commit: ImageCommit)
}

data class ImageCommit(
    val image: StoredImage?,
    internal val createdImagePaths: List<String> = emptyList(),
)

class FilePendingImageCommitter(
    private val filesRoot: File,
    private val imageRoot: File,
    private val cleanupQueue: ImageOrphanCleanupQueue,
) : PendingImageCommitter {
    override fun commit(
        image: StoredImage?,
        errorItemId: ErrorItemId,
    ): ImageCommit {
        if (image == null || !image.relativePath.startsWith(TEMP_PREFIX)) {
            return ImageCommit(image)
        }
        requireSafeTempPath(image.relativePath)
        image.thumbnailRelativePath?.let(::requireSafeTempPath)

        val created = mutableListOf<String>()
        return try {
            val imageName = "${errorItemId.value}.jpg"
            moveToImageRoot(image.relativePath, imageName)
            created += imageName

            val thumbnailName = image.thumbnailRelativePath?.let { thumbnailPath ->
                "${errorItemId.value}-thumb.jpg".also { targetName ->
                    moveToImageRoot(thumbnailPath, targetName)
                    created += targetName
                }
            }
            ImageCommit(
                image = image.copy(
                    relativePath = "images/$imageName",
                    thumbnailRelativePath = thumbnailName?.let { "images/$it" },
                ),
                createdImagePaths = created,
            )
        } catch (error: Throwable) {
            rollback(ImageCommit(image = null, createdImagePaths = created))
            throw error
        }
    }

    override fun rollback(commit: ImageCommit) {
        commit.createdImagePaths.forEach { relativePath ->
            val target = File(imageRoot, relativePath)
            if (target.exists() && !target.delete()) {
                cleanupQueue.enqueue(relativePath)
            }
        }
    }

    private fun moveToImageRoot(
        sourceRelativePath: String,
        targetName: String,
    ) {
        require(imageRoot.exists() || imageRoot.mkdirs()) {
            "Unable to create image directory"
        }
        val source = File(filesRoot, sourceRelativePath)
        require(source.isFile) { "Pending image does not exist" }
        val target = File(imageRoot, targetName)
        require(!target.exists()) { "Committed image already exists" }
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath())
        }
    }

    private fun requireSafeTempPath(relativePath: String) {
        val normalized = File(filesRoot, relativePath).canonicalFile
        val tempRoot = File(filesRoot, TEMP_DIRECTORY).canonicalFile
        require(normalized.path.startsWith("${tempRoot.path}${File.separator}")) {
            "Pending image must be inside the app temp directory"
        }
    }

    companion object {
        private const val TEMP_DIRECTORY = "temp"
        private const val TEMP_PREFIX = "$TEMP_DIRECTORY/"
    }
}
