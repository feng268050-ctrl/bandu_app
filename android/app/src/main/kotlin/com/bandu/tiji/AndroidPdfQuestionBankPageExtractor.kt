package com.bandu.tiji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.bandu.tiji.feature.questionbank.PdfQuestionBankDocument
import com.bandu.tiji.feature.questionbank.PdfQuestionBankPage
import com.bandu.tiji.feature.questionbank.PdfQuestionBankPageExtractor
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPdfQuestionBankPageExtractor(
    context: Context,
    private val maxPages: Int = DEFAULT_MAX_PAGES,
) : PdfQuestionBankPageExtractor {
    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun extract(uri: String): PdfQuestionBankDocument =
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val descriptor = contentResolver.openFileDescriptor(parsed, "r")
                ?: return@withContext PdfQuestionBankDocument(totalPageCount = 0, pages = emptyList())
            descriptor.use { fileDescriptor ->
                PdfRenderer(fileDescriptor).use { renderer ->
                    val pageCount = renderer.pageCount
                    val pages = (0 until pageCount.coerceAtMost(maxPages)).map { index ->
                        renderer.openPage(index).use { page ->
                            page.renderToQuestionBankPage(index + 1)
                        }
                    }
                    PdfQuestionBankDocument(
                        totalPageCount = pageCount,
                        pages = pages,
                    )
                }
            }
        }

    private fun PdfRenderer.Page.renderToQuestionBankPage(pageNumber: Int): PdfQuestionBankPage {
        val scale = minOf(
            MAX_RENDER_SCALE,
            MAX_RENDER_WIDTH.toFloat() / width.toFloat(),
            MAX_RENDER_HEIGHT.toFloat() / height.toFloat(),
        ).coerceAtLeast(MIN_RENDER_SCALE)
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.WHITE)
        render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        val bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
        bitmap.recycle()
        return PdfQuestionBankPage(
            pageNumber = pageNumber,
            imageBytes = bytes,
            mimeType = JPEG_MIME_TYPE,
        )
    }

    private companion object {
        const val DEFAULT_MAX_PAGES = 20
        const val MAX_RENDER_WIDTH = 1800
        const val MAX_RENDER_HEIGHT = 2400
        const val MAX_RENDER_SCALE = 3f
        const val MIN_RENDER_SCALE = 1f
        const val JPEG_QUALITY = 86
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
