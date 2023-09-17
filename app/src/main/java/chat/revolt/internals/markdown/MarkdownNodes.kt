package chat.revolt.internals.markdown

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.discord.simpleast.core.node.Node

class UserMentionNode(private val userId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        val content = renderContext.memberMap[userId]?.let { "@$it" }
            ?: renderContext.userMap[userId]?.let { "@${it.username}" }
            ?: "<@${userId}>"

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

class CustomEmoteNode(private val emoteId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        builder.append(
            renderContext.emojiMap[emoteId]?.let { ":${it.name}:" }
                ?: ":${emoteId}:"
        )
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