package chat.revolt.api.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id")
    val id: String? = null,
    val username: String? = null,
    val discriminator: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
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
) {
    fun mergeWithPartial(partial: User): User {
        return User(
            id = partial.id ?: id,
            username = partial.username ?: username,
            discriminator = partial.discriminator ?: discriminator,
            displayName = partial.displayName ?: displayName,
            avatar = partial.avatar ?: avatar,
            relations = partial.relations ?: relations,
            badges = partial.badges ?: badges,
            status = partial.status ?: status,
            profile = partial.profile ?: profile,
            flags = partial.flags ?: flags,
            privileged = partial.privileged ?: privileged,
            bot = partial.bot ?: bot,
            relationship = partial.relationship ?: relationship,
            online = partial.online ?: online
        )
    }

    companion object {
        fun getPlaceholder(forId: String) = User(
            id = forId,
            username = "Unknown User",
            discriminator = "0000",
            displayName = null,
            avatar = null,
            badges = 0,
            status = null,
            profile = null,
            flags = 0,
            privileged = false,
            bot = null,
            relationship = null,
            online = false
        )

        fun resolveDefaultName(user: User, withDiscriminator: Boolean = false): String {
            val maybeDiscriminator = if (withDiscriminator) "#${user.discriminator}" else ""
            return user.displayName ?: "${user.username}$maybeDiscriminator"
        }
    }
}

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
