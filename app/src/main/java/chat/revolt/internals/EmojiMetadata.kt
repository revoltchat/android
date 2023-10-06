package chat.revolt.internals

import android.content.Context
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.api.RevoltJson
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

enum class EmojiCategory(val googleName: String, val nameResource: Int) {
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

sealed class EmojiPickerItem {
    data class Emoji(val emoji: String) : EmojiPickerItem()
    data class Category(val category: EmojiCategory) : EmojiPickerItem()
}

class EmojiMetadata {
    private var metadata: List<EmojiGroup>

    private fun initMetadata(context: Context): List<EmojiGroup> {
        val json = context.assets.open("metadata/emoji.json").use {
            it.reader().readText()
        }
        return RevoltJson.decodeFromString(ListSerializer(EmojiGroup.serializer()), json)
    }

    fun pickerList(): Map<EmojiCategory, List<Emoji>> {
        val map = mutableMapOf<EmojiCategory, List<Emoji>>()

        for (group in metadata) {
            val category = EmojiCategory.entries.find { it.name == group.group } ?: continue
            map[category] = group.emoji
        }

        return map
    }

    fun flatPickerList(): List<EmojiPickerItem> {
        val list = mutableListOf<EmojiPickerItem>()

        for (group in metadata) {
            val category = EmojiCategory.entries.find { it.googleName == group.group } ?: continue
            list.add(EmojiPickerItem.Category(category))
            list.addAll(group.emoji.map { emoji ->
                EmojiPickerItem.Emoji(
                    emoji.base.joinToString("") { String(Character.toChars(it.toInt())) }
                )
            })
        }

        return list
    }

    init {
        metadata = initMetadata(RevoltApplication.instance.applicationContext)
    }
}