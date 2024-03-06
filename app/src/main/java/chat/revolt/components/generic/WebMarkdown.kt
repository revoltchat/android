package chat.revolt.components.generic

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import chat.revolt.activities.InviteActivity
import chat.revolt.api.REVOLT_APP

private fun argbAsCssColour(argb: Int): String {
    val alpha = (argb shr 24 and 0xff) / 255.0f
    val red = argb shr 16 and 0xff
    val green = argb shr 8 and 0xff
    val blue = argb and 0xff
    return String.format("#%02x%02x%02x%02x", red, green, blue, (alpha * 255).toInt())
}

/**
 * WebView-backed Markdown renderer that supports all Markdown features
 * including KaTeX
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    maskLoading: Boolean = false,
    simpleLineBreaks: Boolean = true,
) {
    val contentColour = LocalContentColor.current
    val materialColourScheme = MaterialTheme.colorScheme

    var finishedLoading by remember { mutableStateOf(false) }

    if (!finishedLoading && maskLoading) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain(Uri.parse(REVOLT_APP).host!!)
                    .addPathHandler(
                        "/_android_assets/",
                        WebViewAssetLoader.AssetsPathHandler(context)
                    )
                    .addPathHandler(
                        "/_android_res/",
                        WebViewAssetLoader.ResourcesPathHandler(context)
                    )
                    .build()

                webChromeClient = object : WebChromeClient() {}
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        webResourceRequest: WebResourceRequest
                    ): Boolean {
                        // Capture clicks on invite links
                        if (webResourceRequest.url.host == "rvlt.gg" ||
                            (
                                    webResourceRequest.url.host?.endsWith("revolt.chat") == true && webResourceRequest.url.path?.startsWith(
                                        "/invite"
                                    ) == true
                                    )
                        ) {
                            val intent = Intent(
                                context,
                                InviteActivity::class.java
                            ).setAction(Intent.ACTION_VIEW)

                            intent.data = webResourceRequest.url
                            context.startActivity(intent)

                            return true
                        }

                        // Otherwise, open the link in the browser using androidx.browser
                        val customTab = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setDefaultColorSchemeParams(
                                CustomTabColorSchemeParams.Builder()
                                    .setToolbarColor(materialColourScheme.background.toArgb())
                                    .build()
                            )
                            .build()
                        customTab.launchUrl(context, webResourceRequest.url)

                        // Prevent the WebView from navigating to the URL
                        return true
                    }
                }

                loadUrl(
                    "$REVOLT_APP/_android_assets/webmarkdown/renderer.html"
                )

                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(false)
                    setSupportMultipleWindows(false)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onLoaded() {
                            finishedLoading = true
                        }

                        @JavascriptInterface
                        fun getMarkdown(): String {
                            return text
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                        }

                        @JavascriptInterface
                        fun getContentColour(): String {
                            return argbAsCssColour(contentColour.toArgb())
                        }

                        @JavascriptInterface
                        fun getPrimaryColour(): String {
                            return argbAsCssColour(materialColourScheme.primary.toArgb())
                        }

                        @JavascriptInterface
                        fun shouldUseSimpleLineBreaks(): Boolean {
                            return simpleLineBreaks
                        }
                    },
                    "Bridge"
                )

                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = {
            it.evaluateJavascript("renderMarkdown()", null)
        }
    )
}
