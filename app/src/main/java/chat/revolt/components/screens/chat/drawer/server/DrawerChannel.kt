package chat.revolt.components.screens.chat.drawer.server

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.api.schemas.ChannelType
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
) {
    val backgroundColor = animateColorAsState(
        if (selected) MaterialTheme.colorScheme.background
        else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        animationSpec = spring()
    )

    val unreadDotOpacity = animateFloatAsState(
        if (hasUnread) 1f else 0f,
        animationSpec = spring()
    )
    val channelAlpha = animateFloatAsState(
        if (hasUnread) 1f else 0.8f,
        animationSpec = spring()
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
        ChannelIcon(channelType = channelType, modifier = Modifier.padding(end = 8.dp))
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