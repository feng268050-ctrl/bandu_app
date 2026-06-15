package com.bandu.tiji.core.designsystem.markdown

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarkdownLatexSecurityTest {
    @Test
    fun `webview disables every external access surface`() {
        val webView = RestrictedMarkdownWebView(
            ApplicationProvider.getApplicationContext(),
        )

        configureRestrictedWebView(webView)

        with(webView.settings) {
            assertThat(javaScriptEnabled).isTrue()
            assertThat(javaScriptCanOpenWindowsAutomatically).isFalse()
            assertThat(supportMultipleWindows()).isFalse()
            assertThat(allowFileAccess).isFalse()
            assertThat(allowContentAccess).isFalse()
            assertThat(allowFileAccessFromFileURLs).isFalse()
            assertThat(allowUniversalAccessFromFileURLs).isFalse()
            assertThat(blockNetworkLoads).isTrue()
            assertThat(loadsImagesAutomatically).isFalse()
            assertThat(domStorageEnabled).isFalse()
            assertThat(databaseEnabled).isFalse()
            assertThat(cacheMode).isEqualTo(WebSettings.LOAD_NO_CACHE)
            assertThat(mixedContentMode).isEqualTo(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            assertThat(mediaPlaybackRequiresUserGesture).isTrue()
        }
        assertThat(CookieManager.getInstance().acceptCookie()).isFalse()
        assertThat(CookieManager.getInstance().acceptThirdPartyCookies(webView)).isFalse()

        webView.destroy()
    }

    @Test
    fun `client blocks navigation and every non markdown asset request`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val client = RestrictedMarkdownWebViewClient(
            assetLoader = MarkdownAssetLoader(context.assets),
            onEntryPointReady = {},
        )

        assertThat(
            client.shouldOverrideUrlLoading(
                null,
                null as WebResourceRequest?,
            ),
        ).isTrue()
        assertThat(
            client.shouldInterceptRequest(
                null,
                FakeWebResourceRequest(Uri.parse("https://example.com/tracker.js")),
            ),
        ).isNotNull()
        assertThat(
            client.shouldInterceptRequest(
                null,
                FakeWebResourceRequest(Uri.parse("file:///sdcard/private.txt")),
            ),
        ).isNotNull()
        assertThat(
            client.shouldInterceptRequest(
                null,
                FakeWebResourceRequest(
                    Uri.parse("file:///android_asset/markdown/renderer.js"),
                ),
            ),
        ).isNotNull()
    }

    @Test
    fun `markdown command remains one JSON encoded string for malicious input`() {
        val malicious = "\");window.location='https://evil.example';//\n<script>alert(1)</script>"
        val script = markdownRenderScript(malicious)

        assertThat(script).startsWith("window.renderMarkdown(\"")
        assertThat(script).endsWith("\");")
        assertThat(script).contains("\\\"")
        assertThat(script).contains("\\n")
        assertThat(script).doesNotContain(
            "window.renderMarkdown(\"\");window.location",
        )
    }

    @Test
    fun `only packaged markdown paths pass the asset allowlist`() {
        assertThat(isAllowedAsset(Uri.parse(MarkdownEntryPoint))).isTrue()
        assertThat(
            isAllowedAsset(
                Uri.parse("file:///android_asset/markdown/vendor/katex/fonts/font.woff2"),
            ),
        ).isTrue()
        assertThat(isAllowedAsset(Uri.parse("content://documents/item"))).isFalse()
        assertThat(isAllowedAsset(Uri.parse("file:///android_asset/other/secret"))).isFalse()
        assertThat(isAllowedAsset(Uri.parse("https://cdn.example.com/library.js"))).isFalse()
    }

    @Test
    fun `asset allowlist rejects traversal encoding and non canonical paths`() {
        val rejected = listOf(
            "file:///android_asset/markdown/../secret.txt",
            "file:///android_asset/markdown/vendor/../renderer.js",
            "file:///android_asset/markdown/%2e%2e/secret.txt",
            "file:///android_asset/markdown/%252e%252e/secret.txt",
            "file:///android_asset/markdown/vendor%2frenderer.js",
            "file:///android_asset/markdown/vendor%5crenderer.js",
            "file:///android_asset/markdown//renderer.js",
            "file:///android_asset/markdown/./renderer.js",
            "file:/android_asset/markdown/renderer.js",
            "file://localhost/android_asset/markdown/renderer.js",
            "file:///android_asset/markdown/renderer.js?remote=true",
            "file:///android_asset/markdown/renderer.js#fragment",
        )

        rejected.forEach { url ->
            assertThat(isAllowedAsset(Uri.parse(url))).isFalse()
        }
    }

    @Test
    fun `entry page and every renderer script load while generic file access is disabled`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val webView = RestrictedMarkdownWebView(context)
        configureRestrictedWebView(webView)
        assertThat(webView.settings.allowFileAccess).isFalse()

        val loader = MarkdownAssetLoader(context.assets)
        val html = loader.load(Uri.parse(MarkdownEntryPoint))
            .requireUtf8Body()
        val scriptSources = Regex("""<script defer src="([^"]+)"></script>""")
            .findAll(html)
            .map { it.groupValues[1] }
            .toList()

        assertThat(scriptSources).containsExactly(
            "vendor/markdown-it-14.1.0.min.js",
            "vendor/katex/katex-0.16.25.min.js",
            "vendor/dompurify-3.3.1.min.js",
            "renderer.js",
        ).inOrder()
        val scripts = scriptSources.associateWith { source ->
            loader.load(Uri.parse("$MarkdownAssetRoot$source"))
                .requireUtf8Body()
        }
        assertThat(scripts.getValue("vendor/markdown-it-14.1.0.min.js"))
            .contains("markdownit")
        assertThat(scripts.getValue("vendor/katex/katex-0.16.25.min.js"))
            .contains("katex")
        assertThat(scripts.getValue("vendor/dompurify-3.3.1.min.js"))
            .contains("DOMPurify")
        assertThat(scripts.getValue("renderer.js"))
            .contains("window.renderMarkdown")

        webView.destroy()
    }
}

private fun android.webkit.WebResourceResponse?.requireUtf8Body(): String {
    checkNotNull(this)
    return data.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private class FakeWebResourceRequest(
    private val requestUri: Uri,
) : WebResourceRequest {
    override fun getUrl(): Uri = requestUri

    override fun isForMainFrame(): Boolean = false

    override fun isRedirect(): Boolean = false

    override fun hasGesture(): Boolean = false

    override fun getMethod(): String = "GET"

    override fun getRequestHeaders(): Map<String, String> = emptyMap()
}
