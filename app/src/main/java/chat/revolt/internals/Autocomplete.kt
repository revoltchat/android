package chat.revolt.internals

import chat.revolt.api.RevoltAPI
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
}