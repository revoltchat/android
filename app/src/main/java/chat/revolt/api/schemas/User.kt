package chat.revolt.api.schemas

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class CompleteUser(
    @SerialName("_id")
    val id: String? = null,

    val username: String? = null,
    val avatar: Avatar? = null,
    val relations: List<Relation>? = null,
    val badges: Long? = null,
    val status: Status? = null,
    val profile: Profile? = null,
    val flags: Long? = null,
    val privileged: Boolean? = null,
    val bot: Bot? = null,
    val relationship: String? = null,
    val online: Boolean? = null
)

@Serializable
data class Avatar(
    @SerialName("_id")
    val id: String? = null,

    val tag: String? = null,
    val filename: String? = null,
    val metadata: Metadata? = null,

    @SerialName("content_type")
    val contentType: String? = null,

    val size: Long? = null,
    val deleted: Boolean? = null,
    val reported: Boolean? = null,

    @SerialName("message_id")
    val messageID: String? = null,

    @SerialName("user_id")
    val userID: String? = null,

    @SerialName("server_id")
    val serverID: String? = null,

    @SerialName("object_id")
    val objectID: String? = null
)

@Serializable
data class Metadata(
    val type: String? = null,
    val width: Long? = null,
    val height: Long? = null
)

@Serializable
data class Bot(
    val owner: String? = null
)

@Serializable
data class Profile(
    val content: String? = null,
    val background: Avatar? = null
)

@Serializable
data class Relation(
    @SerialName("_id")
    val id: String? = null,

    val status: String? = null
)

@Serializable
data class Status(
    val text: String? = null,
    val presence: String? = null
)
