package chat.revolt.internals.markdown

import android.content.Intent
import android.net.Uri
import android.text.TextPaint
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import chat.revolt.activities.InviteActivity
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.REVOLT_INVITES
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LinkSpan(private val url: String, private val drawBackground: Boolean = false) :
    LongClickableSpan() {
    override fun onClick(widget: View) {
        val uri = Uri.parse(url)

        // Intercept invite links
        if (uri.host == Uri.parse(REVOLT_INVITES).host!! ||
            (uri.host?.endsWith(Uri.parse(REVOLT_APP).host!!) == true && uri.path?.startsWith(
                "/invite"
            ) == true)
        ) {
            val intent = Intent(
                widget.context,
                InviteActivity::class.java
            ).setAction(Intent.ACTION_VIEW)

            intent.data = uri
            widget.context.startActivity(intent)

            return
        }

        if (url.startsWith("revolt-android://link-action")) {
            // parse action
            val action = uri.pathSegments[0]

            when (action) {
                "user" -> {
                    val userId = uri.getQueryParameter("user")
                    val serverId = uri.getQueryParameter("server")

                    runBlocking(Dispatchers.IO) {
                        ActionChannel.send(Action.OpenUserSheet(userId!!, serverId))
                    }
                }

                "channel" -> {
                    val channelId = uri.getQueryParameter("channel")

                    runBlocking(Dispatchers.IO) {
                        ActionChannel.send(Action.SwitchChannel(channelId!!))
                    }
                }
            }

            return
        }

        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTab.launchUrl(widget.context, Uri.parse(url))
    }

    override fun onLongClick(view: View?) {
        runBlocking(Dispatchers.IO) {
            ActionChannel.send(Action.LinkInfo(url))
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = false
        if (drawBackground) {
            ds.bgColor = ds.linkColor and 0x33ffffff // 20% alpha
        }
    }
}