package chat.revolt.internals.markdown

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Log
import chat.revolt.api.REVOLT_FILES
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.discord.simpleast.core.node.Node
import kotlin.math.min

class UserMentionNode(private val userId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        val content = renderContext.memberMap[userId]?.let { "@$it" }
            ?: renderContext.userMap[userId]?.let { "@${it.username}" }
            ?: "<@$userId>"

        builder.append(content)
        builder.setSpan(
            LinkSpan(
                "revolt-android://link-action/user?user=$userId${
                    renderContext.serverId?.let {
                        "&server=$it"
                    }.orEmpty()
                }",
                drawBackground = true
            ),
            builder.length - content.length,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

class ChannelMentionNode(private val channelId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        val content = renderContext.channelMap[channelId]?.let { "#$it" }
            ?: "<#$channelId>"

        builder.append(content)
        builder.setSpan(
            LinkSpan(
                "revolt-android://link-action/channel?channel=$channelId",
                drawBackground = true
            ),
            builder.length - content.length,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

class CustomEmoteNode(private val emoteId: String, private val context: Context) :
    Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        val content = renderContext.emojiMap[emoteId]?.let { ":${it.name}:" }
            ?: ":$emoteId:"
        val isGif = renderContext.emojiMap[emoteId]?.animated ?: false
        val emoteUrl = "$REVOLT_FILES/emojis/$emoteId/emote${if (isGif) ".gif" else ".png"}"

        val density = context.resources.displayMetrics.density.toInt()

        builder.append(content)

        try {
            Glide.with(context)
                .asDrawable()
                .load(emoteUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(object : CustomTarget<Drawable>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // no-op
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                    ) {
                        if (resource is GifDrawable) {
                            resource.apply {
                                setLoopCount(GifDrawable.LOOP_FOREVER)
                                start()
                            }
                        }

                        val targetSize = if (renderContext.useLargeEmojis) 48 else 22
                        val maxWidth = if (renderContext.useLargeEmojis) 58 else 38

                        val wantWidth = min(
                            (resource.intrinsicWidth * (targetSize * density)) / resource.intrinsicHeight,
                            maxWidth * density
                        )
                        val wantHeight = targetSize * density

                        resource.setBounds(0, 0, wantWidth, wantHeight)

                        builder.setSpan(
                            EmoteSpan(resource),
                            builder.length - content.length,
                            builder.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            EmoteClickableSpan(emoteId),
                            builder.length - content.length,
                            builder.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                })
        } catch (e: Exception) {
            Log.e("CustomEmoteNode", "Failed to load emote", e)
        }
    }
}

class LinkNode(val content: String, val url: String = content) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        builder.append(content)
        builder.setSpan(
            LinkSpan(url),
            builder.length - content.length,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
