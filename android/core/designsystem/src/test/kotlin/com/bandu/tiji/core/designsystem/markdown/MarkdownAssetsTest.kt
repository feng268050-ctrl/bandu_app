package com.bandu.tiji.core.designsystem.markdown

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarkdownAssetsTest {
    private val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets

    @Test
    fun `fixed renderer versions are packaged as local assets`() {
        val versions = readText("markdown/versions.json")

        assertThat(versions).contains("\"markdown-it\": \"14.1.0\"")
        assertThat(versions).contains("\"katex\": \"0.16.25\"")
        assertThat(versions).contains("\"dompurify\": \"3.3.1\"")
        assertAssetContains(
            "markdown/vendor/markdown-it-14.1.0.min.js",
            "markdown-it 14.1.0",
        )
        assertAssetContains(
            "markdown/vendor/katex/katex-0.16.25.min.js",
            "0.16.25",
        )
        assertAssetContains(
            "markdown/vendor/dompurify-3.3.1.min.js",
            "DOMPurify 3.3.1",
        )
        assertThat(assets.list("markdown/vendor/katex/fonts").orEmpty().size)
            .isAtLeast(20)
    }

    @Test
    fun `renderer entry point references only bundled resources`() {
        val html = readText("markdown/index.html")

        assertThat(html).contains("vendor/markdown-it-14.1.0.min.js")
        assertThat(html).contains("vendor/katex/katex-0.16.25.min.js")
        assertThat(html).contains("vendor/dompurify-3.3.1.min.js")
        assertThat(html).contains("vendor/katex/katex-0.16.25.min.css")
        assertThat(html).contains("renderer.js")
        assertThat(html).doesNotContain("https://")
        assertThat(html).doesNotContain("http://")
        assertThat(html).doesNotContain("src=\"//")
        assertThat(html).doesNotContain("href=\"//")
        assertThat(html).contains("connect-src 'none'")
    }

    @Test
    fun `renderer sanitizes active content images and links`() {
        val renderer = readText("markdown/renderer.js")

        assertThat(renderer).contains("html: false")
        assertThat(renderer).contains("linkify: false")
        assertThat(renderer).contains("markdown.renderer.rules.image = () => ''")
        assertThat(renderer).contains("window.DOMPurify.sanitize")
        assertThat(renderer).contains("'script'")
        assertThat(renderer).contains("'iframe'")
        assertThat(renderer).contains("'img'")
        assertThat(renderer).contains("'a'")
        assertThat(renderer).contains("event.preventDefault()")
    }

    @Test
    fun `renderer supports tables inline math and block math locally`() {
        val html = readText("markdown/index.html")
        val renderer = readText("markdown/renderer.js")

        assertThat(renderer).contains("markdown.enable('table')")
        assertThat(renderer).contains("'math_inline'")
        assertThat(renderer).contains("'math_block'")
        assertThat(renderer).contains("window.katex.renderToString")
        assertThat(renderer).contains("displayMode")
        assertThat(renderer).contains("trust: false")
        assertThat(renderer).contains("throwOnError: false")
        assertThat(html).contains("border-collapse: collapse")
        assertThat(html).contains(".math-block")
    }

    private fun assertAssetContains(path: String, expected: String) {
        assets.open(path).use { input ->
            assertThat(input.readUtf8()).contains(expected)
        }
    }

    private fun readText(path: String): String =
        assets.open(path).use { input -> input.readUtf8() }

    private fun InputStream.readUtf8(): String =
        bufferedReader(Charsets.UTF_8).use { it.readText() }
}
