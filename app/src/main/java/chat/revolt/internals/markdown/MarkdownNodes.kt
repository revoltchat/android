package chat.revolt.internals.markdown

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.util.Log
import com.discord.simpleast.core.node.Node
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

class TimestampNode(private val timestamp: Long, private val modifier: String? = null) :
    Node<MarkdownContext>() {
    override fun render(builder: SpannableStringBuilder, renderContext: MarkdownContext) {
        val normalisedModifier = modifier.orEmpty().removePrefix(":")

        val instant = Instant.fromEpochSeconds(timestamp)
        val javaInstant = instant.toJavaInstant()

        try {
            if (timestamp < 0) {
                builder.append("<invalid timestamp>")
                return
            }

            val outString = when (normalisedModifier) {
                // 22:22
                "t" -> DateTimeFormatter.ofPattern("HH:mm")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(javaInstant)

                // 22:22:22
                "T" -> DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(javaInstant)

                // 22 September 2022
                "D" -> DateTimeFormatter.ofPattern("dd MMMM yyyy")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(javaInstant)

                // 22 September 2022 22:22
                "f" -> DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(javaInstant)

                // Thursday, 22 September 2022 22:22
                "F" -> DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(javaInstant)

                // 9 months ago
                "R" -> DateUtils.getRelativeTimeSpanString(
                    timestamp * 1000,
                    Clock.System.now().toEpochMilliseconds(),
                    DateUtils.MINUTE_IN_MILLIS
                )

                // Fallback. Shouldn't happen
                else -> timestamp.toString()
            }

            builder.append(outString)
        } catch (e: Exception) {
            Log.e("TimestampNode", "Failed to parse timestamp", e)
            builder.append("<invalid timestamp>")
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