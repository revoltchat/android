package chat.revolt.components.screens.chat.drawer.server

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.components.generic.RemoteImage

@Composable
fun DrawerServer(
    iconId: String?,
    serverName: String,
    hasUnreads: Boolean,
    onClick: () -> Unit
) {
    val unreadIndicatorAlpha = animateFloatAsState(
        if (hasUnreads) 1f else 0f,
        animationSpec = spring()
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
                    .clickable(onClick = onClick),
                description = serverName
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = serverName.first().uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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