package chat.revolt.internals

import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.chat.AutocompleteSuggestion

object Autocomplete {
    private val emojiImpl = EmojiImpl()

    fun emoji(query: String): List<AutocompleteSuggestion.Emoji> {
        val unicodeResults = emojiImpl.shortcodeContains(query).map {
            AutocompleteSuggestion.Emoji(
                it.shortcodes.find { shortcode -> shortcode.contains(query) }
                    ?: it.shortcodes.first(),
                it.base.joinToString("") { s -> String(Character.toChars(s.toInt())) },
                null,
                query
            )
        }.distinctBy { it.shortcode }

        val customResults =
            RevoltAPI.emojiCache.values.filter { it.name?.contains(query) ?: false }.map {
                if (it.name != null) {
                    AutocompleteSuggestion.Emoji(
                        ":${it.id}:",
                        null,
                        it,
                        query
                    )
                } else {
                    null
                }
            }.filterNotNull().distinctBy { it.custom?.id }

        return (unicodeResults + customResults)
    }

    fun user(
        channelId: String,
        serverId: String? = null,
        query: String
    ): List<AutocompleteSuggestion.User> {
        val channel = RevoltAPI.channelCache[channelId] ?: return emptyList()

        return when (channel.channelType) {
            ChannelType.DirectMessage -> {
                val otherUser = channel.recipients?.find { it != RevoltAPI.selfId }
                if (otherUser != null) {
                    val user = RevoltAPI.userCache[otherUser]
                    if (user != null && user.username?.contains(query) == true) {
                        listOf(
                            AutocompleteSuggestion.User(
                                user,
                                null,
                                query
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            ChannelType.Group -> {
                val users =
                    channel.recipients?.mapNotNull { RevoltAPI.userCache[it] } ?: emptyList()
                users
                    .filter { it.username?.contains(query) ?: false }
                    .map {
                        AutocompleteSuggestion.User(
                            it,
                            null,
                            query
                        )
                    }
            }

            ChannelType.SavedMessages -> {
                val user = RevoltAPI.userCache[RevoltAPI.selfId]
                return if (user != null && user.username?.contains(query) == true) {
                    listOf(
                        AutocompleteSuggestion.User(
                            user,
                            null,
                            query
                        )
                    )
                } else {
                    emptyList()
                }
            }

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                if (serverId == null) return emptyList()
                if (query.length < 2) return emptyList()

                val byNickname = RevoltAPI.members.filterNamesFor(serverId, query)
                    .map { m -> m to RevoltAPI.userCache[m.id?.user] }.filter { (_, u) ->
                        u != null
                    }.map { (m, u) ->
                        m to u!!
                    }
                val byUsername = RevoltAPI.userCache.values.filter {
                    it.username?.contains(
                        query,
                        ignoreCase = true
                    ) ?: false
                }.mapNotNull {
                    it.id?.let { _ ->
                        RevoltAPI.members.getMember(
                            serverId,
                            it.id
                        ) to it
                    }
                }.filter { (member, _) ->
                    member != null
                }.map { (member, user) ->
                    member!! to user
                }

                val all = (byNickname + byUsername).distinctBy { it.first.id }

                all.map {
                    AutocompleteSuggestion.User(
                        it.second,
                        it.first,
                        query
                    )
                }
            }

            null -> emptyList()
        }
    }
}