package chat.revolt.components.screens.chat.drawer.server

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.components.generic.IconPlaceholder
import chat.revolt.components.generic.RemoteImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerServer(
    iconId: String?,
    serverName: String,
    hasUnreads: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val unreadIndicatorAlpha = animateFloatAsState(
        if (hasUnreads) 1f else 0f,
        animationSpec = spring(),
        label = "Unread indicator alpha"
    )

    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        if (iconId != null) {
            RemoteImage(
                url = "$REVOLT_FILES/icons/${iconId}/server.png?max_side=256",
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                description = serverName
            )
        } else {
            IconPlaceholder(
                name = serverName,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }

        // Unread indicator
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(8.dp)
                .offset(x = (-12).dp)
                .clip(CircleShape)
                .alpha(unreadIndicatorAlpha.value)
                .background(LocalContentColor.current)
        )
    }
}