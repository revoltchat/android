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
data class ServerUserChoice(
    val server: String,
    val user: String,
)

@Serializable
data class Member(
    @SerialName("_id")
    val id: ServerUserChoice? = null,

    @SerialName("joined_at")
    val joinedAt: String? = null,

    val avatar: AutumnResource? = null,
    val roles: List<String>? = null,
    val nickname: String? = null
)

@Serializable
data class Channel(
    @SerialName("_id")
    val id: String? = null,
    @SerialName("channel_type")
    val channelType: ChannelType? = null,
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
    @SerialName("role_permissions")
    val rolePermissions: Map<String, DefaultPermissions>? = null,
    @SerialName("default_permissions")
    val defaultPermissions: DefaultPermissions? = null,
    val nsfw: Boolean? = null,
    val type: String? = null, // this is _only_ used for websocket events!
) {
    fun mergeWithPartial(partial: Channel): Channel {
        return Channel(
            channelType = partial.channelType ?: channelType,
            id = partial.id ?: id,
            user = partial.user ?: user,
            name = partial.name ?: name,
            owner = partial.owner ?: owner,
            description = partial.description ?: description,
            recipients = partial.recipients ?: recipients,
            icon = partial.icon ?: icon,
            lastMessageID = partial.lastMessageID ?: lastMessageID,
            active = partial.active ?: active,
            permissions = partial.permissions ?: permissions,
            server = partial.server ?: server,
            rolePermissions = partial.rolePermissions ?: rolePermissions,
            defaultPermissions = partial.defaultPermissions ?: defaultPermissions,
            nsfw = partial.nsfw ?: nsfw,
            type = partial.type ?: type
        )
    }
}

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