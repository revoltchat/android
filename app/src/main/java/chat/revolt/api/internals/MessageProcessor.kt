package chat.revolt.api.internals

import chat.revolt.api.RevoltAPI

object MessageProcessor {
    private val MentionRegex = Regex("@((?:\\p{L}|[\\d_.-])+)#([0-9]{4})", RegexOption.IGNORE_CASE)

    fun processOutgoing(content: String): String {
        val mentions = MentionRegex.findAll(content).map { it.value }.toList()

        return mentions.fold(content) { acc, mention ->
            val (username, discriminator) = MentionRegex.matchEntire(mention)?.destructured
                ?: return@fold acc

            val user =
                RevoltAPI.userCache.values.find { it.username == username && it.discriminator == discriminator }

            val userId = user?.id ?: return@fold acc
            acc.replace(mention, "<@$userId>")
        }
    }
}