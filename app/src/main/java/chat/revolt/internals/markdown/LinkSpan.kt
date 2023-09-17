package chat.revolt.internals.markdown

import android.net.Uri
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent

class LinkSpan(private val url: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTab.launchUrl(widget.context, Uri.parse(url))
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = false
    }
}