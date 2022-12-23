package chat.revolt.api.schemas

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class MessagesInChannel(
    val messages: List<Message>? = null,
    val users: List<User>? = null,
    val members: List<Member>? = null
)

@Serializable
data class Member(
    @SerialName("_id")
    val id: String? = null,

    @SerialName("joined_at")
    val joinedAt: String? = null,

    val avatar: AutumnResource? = null,
    val roles: List<String>? = null,
    val nickname: String? = null
)

@Serializable
data class Channel(
    val channelType: ChannelType? = null,
    @SerialName("_id")
    val id: String? = null,
    val user: String? = null,
    val name: String? = null,
    val owner: String? = null,
    val description: String? = null,
    val recipients: List<String>? = null,
    val icon: AutumnResource? = null,
    val lastMessageID: String? = null,
    val active: Boolean? = null,
    val permissions: Long? = null,
    val server: String? = null,
    val rolePermissions: Map<String, DefaultPermissions>? = null,
    val defaultPermissions: DefaultPermissions? = null,
    val nsfw: Boolean? = null,
    val type: String? = null, // this is _only_ used for websocket events!
)

@Serializable
enum class ChannelType(val value: String) {
    DirectMessage("DirectMessage"),
    Group("Group"),
    SavedMessages("SavedMessages"),
    TextChannel("TextChannel"),
    VoiceChannel("VoiceChannel");

    companion object : KSerializer<ChannelType> {
        override val descriptor: SerialDescriptor
            get() {
                return PrimitiveSerialDescriptor(
                    "chat.revolt.api.schemas.ChannelType",
                    PrimitiveKind.STRING
                )
            }

        override fun deserialize(decoder: Decoder): ChannelType =
            when (val value = decoder.decodeString()) {
                "DirectMessage" -> DirectMessage
                "Group" -> Group
                "SavedMessages" -> SavedMessages
                "TextChannel" -> TextChannel
                "VoiceChannel" -> VoiceChannel
                else -> throw IllegalArgumentException("ChannelType could not parse: $value")
            }

        override fun serialize(encoder: Encoder, value: ChannelType) {
            return encoder.encodeString(value.value)
        }
    }
}