package com.bandu.tiji.core.designsystem.markdown

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Color
import android.net.Uri
import android.webkit.ClientCertRequest
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.HttpAuthHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException

internal const val MarkdownAssetRoot = "file:///android_asset/markdown/"
internal const val MarkdownEntryPoint = "${MarkdownAssetRoot}index.html"
internal const val MarkdownStreamingRefreshMillis = 250L

@Composable
fun MarkdownLatexView(
    markdown: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
) {
    val batchedMarkdown = rememberBatchedMarkdown(
        markdown = markdown,
        streaming = streaming,
    )
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RestrictedMarkdownWebView(context).apply {
                configureRestrictedWebView(this)
                loadUrl(MarkdownEntryPoint)
            }
        },
        update = { webView ->
            webView.submitMarkdown(batchedMarkdown)
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        },
    )
}

@Composable
internal fun rememberBatchedMarkdown(
    markdown: String,
    streaming: Boolean,
): String {
    var batchedMarkdown by remember { mutableStateOf(markdown) }
    val latestMarkdown = rememberUpdatedState(markdown)

    LaunchedEffect(markdown, streaming) {
        if (!streaming) {
            batchedMarkdown = markdown
        }
    }
    LaunchedEffect(streaming) {
        while (streaming) {
            delay(MarkdownStreamingRefreshMillis)
            val latest = latestMarkdown.value
            if (latest != batchedMarkdown) {
                batchedMarkdown = latest
            }
        }
    }
    return if (streaming) batchedMarkdown else markdown
}

internal fun configureRestrictedWebView(webView: RestrictedMarkdownWebView) {
    webView.setBackgroundColor(Color.TRANSPARENT)
    webView.isVerticalScrollBarEnabled = false
    webView.isHorizontalScrollBarEnabled = false
    webView.isLongClickable = false
    webView.setOnLongClickListener { true }
    webView.setDownloadListener { _, _, _, _, _ -> Unit }

    webView.settings.apply {
        javaScriptEnabled = true
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        blockNetworkLoads = true
        loadsImagesAutomatically = false
        domStorageEnabled = false
        databaseEnabled = false
        setGeolocationEnabled(false)
        cacheMode = WebSettings.LOAD_NO_CACHE
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        mediaPlaybackRequiresUserGesture = true
        safeBrowsingEnabled = true
    }

    CookieManager.getInstance().apply {
        setAcceptCookie(false)
        setAcceptThirdPartyCookies(webView, false)
    }
    webView.webChromeClient = RestrictedMarkdownWebChromeClient()
    webView.webViewClient = RestrictedMarkdownWebViewClient(
        assetLoader = MarkdownAssetLoader(webView.context.assets),
        onEntryPointReady = RestrictedMarkdownWebView::markEntryPointReady,
    )
}

internal class RestrictedMarkdownWebView(context: Context) : WebView(context) {
    private var entryPointReady = false
    private var pendingMarkdown = ""

    fun submitMarkdown(markdown: String) {
        pendingMarkdown = markdown
        renderPendingMarkdown()
    }

    fun markEntryPointReady() {
        entryPointReady = true
        renderPendingMarkdown()
    }

    private fun renderPendingMarkdown() {
        if (!entryPointReady) return
        evaluateJavascript(markdownRenderScript(pendingMarkdown), null)
    }
}

internal class RestrictedMarkdownWebViewClient(
    private val assetLoader: MarkdownAssetLoader,
    private val onEntryPointReady: (RestrictedMarkdownWebView) -> Unit,
) : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String?) {
        if (url == MarkdownEntryPoint) {
            onEntryPointReady(view as RestrictedMarkdownWebView)
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean = true

    @Deprecated("Deprecated in WebViewClient")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest,
    ): WebResourceResponse? =
        assetLoader.load(request.url) ?: emptyBlockedResponse()

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler,
        error: android.net.http.SslError?,
    ) {
        handler.cancel()
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler,
        host: String?,
        realm: String?,
    ) {
        handler.cancel()
    }

    override fun onReceivedClientCertRequest(
        view: WebView?,
        request: ClientCertRequest,
    ) {
        request.cancel()
    }
}

internal class MarkdownAssetLoader(
    private val assets: AssetManager,
) {
    fun load(uri: Uri): WebResourceResponse? {
        val assetPath = markdownAssetPath(uri) ?: return null
        return try {
            WebResourceResponse(
                mimeTypeFor(assetPath),
                encodingFor(assetPath),
                assets.open(assetPath, AssetManager.ACCESS_STREAMING),
            )
        } catch (_: IOException) {
            emptyBlockedResponse()
        }
    }
}

internal class RestrictedMarkdownWebChromeClient : WebChromeClient() {
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?,
    ): Boolean = false

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback,
    ) {
        callback.invoke(origin, false, false)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean = false

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult,
    ): Boolean {
        result.cancel()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult,
    ): Boolean {
        result.cancel()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean {
        result.cancel()
        return true
    }
}

internal fun markdownRenderScript(markdown: String): String =
    "window.renderMarkdown(${JSONObject.quote(markdown)});"

internal fun isAllowedAsset(uri: Uri): Boolean = markdownAssetPath(uri) != null

internal fun markdownAssetPath(uri: Uri): String? {
    val encoded = uri.encodedPath ?: return null
    if (uri.scheme != "file" || !uri.authority.isNullOrEmpty()) return null
    if (uri.query != null || uri.fragment != null || uri.encodedQuery != null) return null
    if (!uri.toString().startsWith(MarkdownAssetRoot)) return null
    if (!encoded.startsWith("/android_asset/markdown/")) return null
    if ('%' in encoded || '\\' in encoded) return null

    val relativePath = encoded.removePrefix("/android_asset/")
    val segments = relativePath.split('/')
    if (segments.size < 2 || segments.any { it.isEmpty() || it == "." || it == ".." }) {
        return null
    }
    return relativePath
}

private fun mimeTypeFor(assetPath: String): String =
    when (assetPath.substringAfterLast('.', missingDelimiterValue = "")) {
        "html" -> "text/html"
        "js" -> "application/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "txt" -> "text/plain"
        "ttf" -> "font/ttf"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }

private fun encodingFor(assetPath: String): String? =
    when (assetPath.substringAfterLast('.', missingDelimiterValue = "")) {
        "html", "js", "css", "json", "txt" -> Charsets.UTF_8.name()
        else -> null
    }

private fun emptyBlockedResponse(): WebResourceResponse =
    WebResourceResponse(
        "text/plain",
        Charsets.UTF_8.name(),
        ByteArrayInputStream(ByteArray(0)),
    )
