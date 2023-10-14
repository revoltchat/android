package chat.revolt.internals

import android.content.Context
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Server
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class Emoji(
    val base: List<Long>,
    val alternates: List<List<Long>>,
    val emoticons: List<String>,
    val shortcodes: List<String>,
    val animated: Boolean,
)

@Serializable
data class EmojiGroup(
    val group: String,
    val emoji: List<Emoji>,
)

enum class UnicodeEmojiSection(val googleName: String, val nameResource: Int) {
    Smileys("Smileys and emotions", R.string.emoji_category_smileys),
    People("People", R.string.emoji_category_people),
    Animals("Animals and nature", R.string.emoji_category_animals),
    Food("Food and drink", R.string.emoji_category_food),
    Travel("Travel and places", R.string.emoji_category_travel),
    Activities("Activities and events", R.string.emoji_category_activities),
    Objects("Objects", R.string.emoji_category_objects),
    Symbols("Symbols", R.string.emoji_category_symbols),
    Flags("Flags", R.string.emoji_category_flags),
}

sealed class Category {
    data class UnicodeEmojiCategory(val definition: UnicodeEmojiSection) : Category()
    data class ServerEmoteCategory(val server: Server) : Category()
}

sealed class EmojiPickerItem {
    data class Section(val category: Category) : EmojiPickerItem()
    data class UnicodeEmoji(val emoji: String) : EmojiPickerItem()
    data class ServerEmote(val emote: chat.revolt.api.schemas.Emoji) : EmojiPickerItem()
}

class EmojiMetadata {
    private var metadata: List<EmojiGroup>

    private fun initMetadata(context: Context): List<EmojiGroup> {
        val json = context.assets.open("metadata/emoji.json").use {
            it.reader().readText()
        }
        return RevoltJson.decodeFromString(ListSerializer(EmojiGroup.serializer()), json)
    }

    fun serversWithEmotes(): List<Server> {
        return RevoltAPI
            .emojiCache
            .values
            .asSequence()
            .map { it.parent }
            .filterNotNull()
            .filter { it.type == "Server" }
            .map { it.id }
            .distinct()
            .mapNotNull { RevoltAPI.serverCache[it] }
            .toList()
    }

    fun serverEmoteList(server: Server): List<EmojiPickerItem> {
        val list = mutableListOf<EmojiPickerItem>()
        val emotes = RevoltAPI.emojiCache.values.filter { it.parent?.id == server.id }

        list.add(EmojiPickerItem.Section(Category.ServerEmoteCategory(server)))
        list.addAll(emotes.map { EmojiPickerItem.ServerEmote(it) })

        return list
    }

    fun flatPickerList(): List<EmojiPickerItem> {
        val list = mutableListOf<EmojiPickerItem>()

        for (server in serversWithEmotes()) {
            list.addAll(serverEmoteList(server))
        }

        for (group in metadata) {
            val category =
                UnicodeEmojiSection.entries.find { it.googleName == group.group } ?: continue
            list.add(EmojiPickerItem.Section(Category.UnicodeEmojiCategory(category)))
            list.addAll(group.emoji.map { emoji ->
                EmojiPickerItem.UnicodeEmoji(
                    emoji.base.joinToString("") { String(Character.toChars(it.toInt())) }
                )
            })
        }

        return list
    }

    /**
     * Returns a map of category to start and end index of the category in the flat picker list
     * Impl
     * ====
     * 1. Iterate through servers that have emotes. Get the index of the server emote category.
     * 2. Get all emotes in that server. Add the size of that list to the index of the server emote category.
     * 3. Push Pair(index, index + size) to the map.
     * 4. Iterate through all unicode emoji categories. Get the index of the category.
     * Unless it's the last category {
     * 5.1. Get the index of the next category. Subtract 1 from that index.
     * 5.2. Push Pair(index, lastIndex) to the map.
     * } Otherwise {
     * 5. Push Pair(index, Int.MAX_VALUE) to the map.
     * }
     * 6. Return the map.
     */
    fun categorySpans(flatPickerList: List<EmojiPickerItem>): Map<Category, Pair<Int, Int>> {
        val output = mutableMapOf<Category, Pair<Int, Int>>()

        for (server in serversWithEmotes()) {
            val index =
                flatPickerList.indexOfFirst { it is EmojiPickerItem.Section && it.category is Category.ServerEmoteCategory && it.category.server == server }
            val allEmotesInThatServer =
                RevoltAPI.emojiCache.values.filter { it.parent?.id == server.id }
            val lastIndex = index + allEmotesInThatServer.size

            output[Category.ServerEmoteCategory(server)] = Pair(index, lastIndex)
        }
        for (section in UnicodeEmojiSection.entries) {
            val index =
                flatPickerList.indexOfFirst { it is EmojiPickerItem.Section && it.category is Category.UnicodeEmojiCategory && it.category.definition == section }
            val lastIndex = if (section == UnicodeEmojiSection.entries.last()) {
                Int.MAX_VALUE
            } else {
                val nextSection = UnicodeEmojiSection.entries[section.ordinal + 1]
                flatPickerList.indexOfFirst { it is EmojiPickerItem.Section && it.category is Category.UnicodeEmojiCategory && it.category.definition == nextSection } - 1
            }
            output[Category.UnicodeEmojiCategory(section)] = Pair(index, lastIndex)
        }

        return output
    }

    init {
        metadata = initMetadata(RevoltApplication.instance.applicationContext)
    }
}