package chat.revolt.api.schemas

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class MessagesInChannel(
    val messages: List<Message>? = null,
    val users: List<CompleteUser>? = null,
    val members: List<Member>? = null
)

@Serializable
data class Member(
    @SerialName("_id")
    val id: String? = null,

    @SerialName("joined_at")
    val joinedAt: String? = null,

    val avatar: Avatar? = null,
    val roles: List<String>? = null,
    val nickname: String? = null
)