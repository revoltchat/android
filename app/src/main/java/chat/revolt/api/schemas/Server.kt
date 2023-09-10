package chat.revolt.api.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Server(
    @SerialName("_id")
    val id: String? = null,
    val owner: String? = null,
    val name: String? = null,
    val description: String? = null,
    val channels: List<String>? = null,
    val categories: List<Category>? = null,
    val systemMessages: SystemMessages? = null,
    val roles: Map<String, Role>? = null,
    val defaultPermissions: Long? = null,
    val icon: AutumnResource? = null,
    val banner: AutumnResource? = null,
    val flags: Long? = null,
    val analytics: Boolean? = null,
    val discoverable: Boolean? = null,
) {
    fun mergeWithPartial(other: Server): Server {
        return Server(
            id = other.id ?: id,
            owner = other.owner ?: owner,
            name = other.name ?: name,
            description = other.description ?: description,
            channels = other.channels ?: channels,
            categories = other.categories ?: categories,
            systemMessages = other.systemMessages ?: systemMessages,
            roles = other.roles ?: roles,
            defaultPermissions = other.defaultPermissions ?: defaultPermissions,
            icon = other.icon ?: icon,
            banner = other.banner ?: banner,
            flags = other.flags ?: flags,
            analytics = other.analytics ?: analytics,
            discoverable = other.discoverable ?: discoverable,
        )
    }
}

@Serializable
data class Category(
    val id: String? = null,
    val title: String? = null,
    val channels: List<String>? = null
)

@Serializable
data class SystemMessages(
    val userJoined: String? = null,
    val userLeft: String? = null,
    val userKicked: String? = null,
    val userBanned: String? = null
)

@Serializable
data class Role(
    val name: String? = null,
    val permissions: PermissionDescription? = null,
    val colour: String? = null,
    val hoist: Boolean? = null,
    val rank: Double? = null
)

@Serializable
data class PermissionDescription(
    val a: Long,
    val d: Long
)

@Serializable
data class Emoji(
    @SerialName("_id")
    val id: String? = null,
    val parent: EmojiParent? = null,
    val creatorID: String? = null,
    val name: String? = null,
    val animated: Boolean? = null,
    val type: String? = null, // this is _only_ used for websocket events!
)

@Serializable
data class EmojiParent(
    val type: String? = null,
    val id: String? = null
)