package chat.revolt.api.schemas

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class User(
    @SerialName("_id")
    val id: String? = null,
    val username: String? = null,
    val avatar: AutumnResource? = null,
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
data class Bot(
    val owner: String? = null
)

@Serializable
data class Profile(
    val content: String? = null,
    val background: AutumnResource? = null
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
