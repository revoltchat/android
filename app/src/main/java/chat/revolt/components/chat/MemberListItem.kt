package chat.revolt.components.chat

import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.schemas.Member
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus

@Composable
fun MemberListItem(
    member: Member?,
    user: User?,
    serverId: String?,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val highestColourRole = serverId?.let {
        user?.id?.let { userId ->
            Roles.resolveHighestRole(
                it,
                userId,
                true
            )
        }
    }

    val colour = highestColourRole?.colour?.let { WebCompat.parseColour(it) }
        ?: Brush.solidColor(LocalContentColor.current)

    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = member?.nickname
                    ?: user?.displayName
                    ?: user?.username
                    ?: user?.id
                    ?: userId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.copy(brush = colour),
            )
        },
        leadingContent = {
            UserAvatar(
                username = member?.nickname
                    ?: user?.displayName
                    ?: user?.username
                    ?: user?.id
                    ?: userId,
                avatar = user?.avatar,
                rawUrl = member?.avatar?.let { "$REVOLT_FILES/avatars/${it.id}?max_side=256" },
                userId = userId,
                presence = presenceFromStatus(
                    user?.status?.presence,
                    user?.online ?: false
                )
            )
        },
    )
}