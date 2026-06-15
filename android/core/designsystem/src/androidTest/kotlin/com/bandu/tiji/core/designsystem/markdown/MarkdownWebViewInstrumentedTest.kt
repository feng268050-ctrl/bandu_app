package com.bandu.tiji.core.designsystem.markdown

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MarkdownWebViewInstrumentedTest {
    @Test
    fun localEntryPointExecutesBundledTableAndKatexRendererWithFileAccessDisabled() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val renderedHtml = AtomicReference<String>()
        val failure = AtomicReference<Throwable>()
        val rendered = CountDownLatch(1)
        lateinit var webView: RestrictedMarkdownWebView

        instrumentation.runOnMainSync {
            try {
                webView = RestrictedMarkdownWebView(instrumentation.targetContext)
                configureRestrictedWebView(webView)
                assertTrue(!webView.settings.allowFileAccess)
                webView.webViewClient = RestrictedMarkdownWebViewClient(
                    assetLoader = MarkdownAssetLoader(webView.context.assets),
                    onEntryPointReady = {
                        it.markEntryPointReady()
                        it.submitMarkdown(
                            """
                            |列 A|列 B|
                            |---|---|
                            |1|2|

                            行内公式：${'$'}x^2${'$'}

                            ${'$'}${'$'}
                            \frac{1}{2}
                            ${'$'}${'$'}
                            """.trimIndent(),
                        )
                        it.evaluateJavascript(
                            """
                            (() => {
                              const content = document.getElementById('content');
                              return JSON.stringify({
                                table: content.querySelectorAll('table').length,
                                katex: content.querySelectorAll('.katex').length,
                                text: content.textContent
                              });
                            })()
                            """.trimIndent(),
                        ) { value ->
                            renderedHtml.set(value)
                            rendered.countDown()
                        }
                    },
                )
                webView.loadUrl(MarkdownEntryPoint)
            } catch (throwable: Throwable) {
                failure.set(throwable)
                rendered.countDown()
            }
        }

        assertTrue(rendered.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw it }
        val result = renderedHtml.get()
        assertTrue(result.contains("\\\"table\\\":1"))
        assertTrue(result.contains("\\\"katex\\\":2"))
        assertTrue(result.contains("列 A"))

        instrumentation.runOnMainSync {
            webView.stopLoading()
            webView.destroy()
        }
    }
}
