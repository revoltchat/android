package chat.revolt.components.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.UserAvatar

@Composable
fun ChannelSheetHeader(
    channelName: String,
    channelIcon: AutumnResource? = null,
    channelType: ChannelType,
    dmPartner: User? = null
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channelIcon != null) {
                RemoteImage(
                    url = "$REVOLT_FILES/icons/${channelIcon.id ?: ""}?max_side=48",
                    description = null, // decorative
                    contentScale = ContentScale.Fit,
                    height = 48,
                    width = 48,
                    modifier = Modifier
                        .size(24.dp)
                )
            } else if (dmPartner != null) {
                UserAvatar(
                    username = User.resolveDefaultName(dmPartner),
                    userId = dmPartner.id ?: "",
                    avatar = dmPartner.avatar,
                    presence = null,
                    size = 48.dp,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else {
                ChannelIcon(channelType = channelType)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = channelName,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
