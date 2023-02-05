package chat.revolt.components.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.AutumnResource

enum class Presence {
    Online,
    Idle,
    Dnd,
    Focus,
    Offline,
}

fun presenceColour(presence: Presence): Color {
    return when (presence) {
        Presence.Online -> Color(0xff73b258)
        Presence.Idle -> Color(0xffecc73c)
        Presence.Dnd -> Color(0xffd24c41)
        Presence.Focus -> Color(0xff69a2ef)
        Presence.Offline -> Color(0xff546e7a)
    }
}

@Composable
fun PresenceBadge(presence: Presence, size: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            .background(presenceColour(presence))
    )
}

@Composable
fun UserAvatar(
    username: String,
    userId: String,
    modifier: Modifier = Modifier,
    presence: Presence? = null,
    avatar: AutumnResource? = null,
    size: Dp = 40.dp,
    presenceSize: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (avatar != null) {
            RemoteImage(
                url = "$REVOLT_FILES/avatars/${avatar.id!!}/user.png",
                description = stringResource(id = R.string.avatar_alt, username),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(size)
            )
        } else {
            RemoteImage(
                url = "$REVOLT_BASE/users/${userId}/default_avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                description = stringResource(id = R.string.avatar_alt, username),
            )
        }

        if (presence != null) {
            PresenceBadge(presence, size = presenceSize)
        }
    }
}

@Composable
fun UserAvatarWidthPlaceholder(
    size: Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .width(size)
    )
}