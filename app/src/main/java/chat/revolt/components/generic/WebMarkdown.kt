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
import org.intellij.lang.annotations.Language

// TODO: Obvious placeholder.
@Language("HTML")
private const val HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width">    
        <title></title>
        <style>
            @font-face {
                font-family: "Inter";
                src: url("/_android_res/font/inter_regular.ttf");
                font-weight: 400;
                font-style: normal;
            }
            
            @font-face {
                font-family: "Inter";
                src: url("/_android_res/font/inter_bold.ttf");
                font-weight: 700;
                font-style: normal;
            }
            
            @font-face {
                font-family: "JetBrains Mono";
                src: url("/_android_res/font/jetbrainsmono_regular.ttf");
                font-weight: 400;
                font-style: normal;
            }
        
            body, html {
                font-family: "Inter", sans-serif;
                margin: 0;
                padding: 0;
                color: %s;
            }
            
            a:link, a:visited {
                color: %s;
                text-decoration: none;
            }
            
            pre, code {
                font-family: "JetBrains Mono", monospace;
            }
            
            #markdown {
                overflow-wrap: break-word;
                word-wrap: break-word;
                word-break: break-word;
                hyphens: auto;
                max-width: 100vw;
                overflow-x: hidden;
            }
        </style>
    </head>
    <body>
        <div id="markdown">%s</div>
        <script src="https://cdn.jsdelivr.net/npm/showdown@2.1.0/dist/showdown.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/dompurify@3.0.5/dist/purify.min.js"></script>
        <script>
            window.addEventListener("load", () => {
                const converter = new showdown.Converter()
                
                converter.setFlavor("github")
                converter.setOption("tables", true)
                converter.setOption("emoji", true)
                converter.setOption("disableForced4SpacesIndentedSublists", true)
                converter.setOption("noHeaderId", true)
                converter.setOption("simpleLineBreaks", true)
                converter.setOption("strikethrough", true)
                converter.setOption("tasklists", true)
                
                const markdown = document.querySelector("#markdown")
                const html = converter.makeHtml(markdown.innerHTML)
                markdown.innerHTML = DOMPurify.sanitize(html)
                Android.onLoaded()
            })
        </script>
    </body>
</html>
"""

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
    maskLoading: Boolean = false,
    modifier: Modifier = Modifier,
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
                val cssContentColour = argbAsCssColour(contentColour.toArgb())
                val cssPrimaryColour = argbAsCssColour(materialColourScheme.primary.toArgb())

                val html = String.format(
                    HTML_TEMPLATE,
                    cssContentColour,
                    cssPrimaryColour,
                    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                )

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
                            (webResourceRequest.url.host?.endsWith("revolt.chat") == true && webResourceRequest.url.path?.startsWith(
                                "/invite"
                            ) == true)
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

                loadDataWithBaseURL(
                    REVOLT_APP,
                    html,
                    "text/html; charset=utf-8",
                    "UTF-8",
                    null
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
                    },
                    "Android"
                )

                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }
        }
    )
}