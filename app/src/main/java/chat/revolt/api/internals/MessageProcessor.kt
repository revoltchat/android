package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.internals.EmojiImpl

object MessageProcessor {
    private val MentionRegex = Regex("@((?:\\p{L}|[\\d_.-])+)#([0-9]{4})", RegexOption.IGNORE_CASE)
    private val ChannelRegex = Regex("(?:\\s|^)#(.+?)(?:\\s|\$)", RegexOption.IGNORE_CASE)
    private val EmojiRegex = Regex(":(.+?):", RegexOption.IGNORE_CASE)

    val emoji = EmojiImpl()

    /**
     * Processes an outgoing message for sending.
     * 1. Replaces @mentions#0000 with <@userId>
     * 2. Replaces #channel with <#channelId> if the current server has a channel with that name
     * 2. Replaces :emoji-shortcode: with the emoji's unicode character, if it exists
     */
    fun processOutgoing(content: String, serverId: String?): String {
        val mentions = MentionRegex.findAll(content).map { it.value }.toList()

        var returnable = mentions.fold(content) { acc, mention ->
            val (username, discriminator) = MentionRegex.matchEntire(mention)?.destructured
                ?: return@fold acc

            val user =
                RevoltAPI.userCache.values.find { it.username == username && it.discriminator == discriminator }

            val userId = user?.id ?: return@fold acc
            acc.replace(mention, "<@$userId>")
        }

        val channels = ChannelRegex.findAll(returnable).map { it.value }.toList()

        returnable = channels.fold(returnable) { acc, channel ->
            val channelName = ChannelRegex.matchEntire(channel)?.destructured?.component1()
                ?: return@fold acc

            val fetchedChannel =
                RevoltAPI.channelCache.values.find {
                    it.name == channelName && it.server == serverId && it.channelType == ChannelType.TextChannel
                }
                    ?: return@fold acc

            fetchedChannel.name?.let { acc.replace("#${it}", "<#${fetchedChannel.id}>") } ?: acc
        }

        val emojis = EmojiRegex.findAll(returnable).map { it.value }.toList()

        returnable = emojis.fold(returnable) { acc, emoji ->
            val emojiName = EmojiRegex.matchEntire(emoji)?.destructured?.component1()
                ?: return@fold acc

            val byShortcode = this.emoji.unicodeByShortcode(emojiName)
                ?: return@fold acc

            acc.replace(":$emojiName:", byShortcode)
        }

        return returnable
    }
}