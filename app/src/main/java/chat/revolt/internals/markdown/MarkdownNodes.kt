package chat.revolt.internals.markdown

import android.text.SpannableStringBuilder
import com.discord.simpleast.core.node.Node

class UserMentionNode(private val userId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        builder.append(
            renderContext.memberMap[userId]?.let { "@$it" }
                ?: renderContext.userMap[userId]?.let { "@${it.username}" }
                ?: "<@${userId}>"
        )
    }
}

class ChannelMentionNode(private val channelId: String) : Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        builder.append(
            renderContext.channelMap[channelId]?.let { "#$it" }
                ?: "<#${channelId}>"
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
