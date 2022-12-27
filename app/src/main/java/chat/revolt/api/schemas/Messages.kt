package chat.revolt.api.schemas

import chat.revolt.api.RevoltAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("_id")
    val id: String? = null,
    val nonce: String? = null,
    val channel: String? = null,
    val author: String? = null,
    val content: String? = null,
    val reactions: Map<String, List<String>>? = null,
    val replies: List<String>? = null,
    val attachments: List<AutumnResource>? = null,
    val edited: String? = null,
    val embeds: List<Embed>? = null,
    val mentions: List<String>? = null,
    val type: String? = null, // this is _only_ used for websocket events!
) {
    fun getAuthor(): User? {
        return author?.let { RevoltAPI.userCache[it] }
    }

    fun mergeWithPartial(partial: Message): Message {
        return Message(
            id = partial.id ?: id,
            nonce = partial.nonce ?: nonce,
            channel = partial.channel ?: channel,
            author = partial.author ?: author,
            content = partial.content ?: content,
            reactions = partial.reactions ?: reactions,
            replies = partial.replies ?: replies,
            attachments = partial.attachments ?: attachments,
            edited = partial.edited ?: edited,
            embeds = partial.embeds ?: embeds,
            mentions = partial.mentions ?: mentions,
            type = partial.type ?: type
        )
    }
}

@Serializable
data class Embed(
    val type: String? = null,
    val url: String? = null,

    @SerialName("original_url")
    val originalURL: String? = null,

    val special: Special? = null,
    val title: String? = null,
    val description: String? = null,
    val image: Image? = null,

    @SerialName("icon_url")
    val iconURL: String? = null,

    @SerialName("site_name")
    val siteName: String? = null,

    val colour: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val size: String? = null
)

@Serializable
data class Image(
    val url: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val size: String? = null
)

@Serializable
data class Special(
    val type: String? = null
)