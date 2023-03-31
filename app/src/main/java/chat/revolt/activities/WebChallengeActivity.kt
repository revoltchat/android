package chat.revolt.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.buildUserAgent
import chat.revolt.databinding.ActivityWebchallengeBinding

private class WebChallengeClient(val pageLoaded: () -> Unit) : WebViewClient() {
    @Override
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        pageLoaded()
    }
}

class WebChallengeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebchallengeBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebchallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = buildUserAgent("WebChallenge")
        }

        binding.webView.webViewClient = WebChallengeClient {
            binding.webView.evaluateJavascript(
                "(function() { return document.getElementById('cf-wrapper') != null; })();"
            ) { result ->
                if (result == "false") { // No challenge
                    finish()
                }
            }
        }

        binding.webView.loadUrl(REVOLT_BASE)
    }
}