package chat.revolt.components.screens.chat.drawer.server

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.generic.GroupIcon
import chat.revolt.components.generic.Presence
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.screens.chat.ChannelIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerChannel(
    channelType: ChannelType,
    name: String,
    selected: Boolean,
    hasUnread: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    dmPartnerStatus: Presence? = null,
    dmPartnerName: String? = null,
    dmPartnerIcon: AutumnResource? = null,
    dmPartnerId: String? = null,
    large: Boolean = false,
) {
    val backgroundColor = animateColorAsState(
        if (selected) MaterialTheme.colorScheme.background
        else Color.Transparent,
        animationSpec = spring(),
        label = "Channel background colour"
    )

    val unreadDotOpacity = animateFloatAsState(
        if (hasUnread) 1f else 0f,
        animationSpec = spring(),
        label = "Unread dot opacity"
    )

    val channelAlpha = animateFloatAsState(
        if (hasUnread || selected) 1f else 0.8f,
        animationSpec = spring(),
        label = "Channel alpha"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor.value)
            .alpha(channelAlpha.value)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (channelType) {
            ChannelType.DirectMessage -> UserAvatar(
                username = dmPartnerName ?: "",
                avatar = dmPartnerIcon,
                userId = dmPartnerId ?: "",
                presence = dmPartnerStatus,
                size = 32.dp,
                presenceSize = 16.dp,
                modifier = Modifier.padding(end = 8.dp)
            )

            ChannelType.Group -> GroupIcon(
                name = name,
                icon = dmPartnerIcon,
                size = 32.dp,
                modifier = Modifier.padding(end = 8.dp)
            )

            else -> ChannelIcon(
                channelType = channelType,
                modifier = Modifier.then(
                    if (large) Modifier.padding(
                        end = 12.dp,
                        start = 4.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    ) else Modifier.padding(end = 8.dp)
                )
            )
        }

        Text(
            text = name,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        if (hasUnread) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp)
                    .clip(CircleShape)
                    .background(LocalContentColor.current)
                    .alpha(unreadDotOpacity.value)
                    .size(8.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}