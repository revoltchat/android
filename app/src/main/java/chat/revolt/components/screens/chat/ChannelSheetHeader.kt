package chat.revolt.components.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.generic.RemoteImage

@Composable
fun ChannelSheetHeader(
    channelName: String,
    channelIcon: AutumnResource? = null,
    channelType: ChannelType,
    channelId: String,
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
                    getIconBackColour(channelId)
                        .copy(alpha = if (channelIcon != null) 0.6f else 0.2f)
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
            } else {
                ChannelIcon(channelType = channelType)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = channelName,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun getIconBackColour(channelId: String): Color {
    // The ULID alphabet does not include I, L, O, or U.
    return when (channelId.uppercase().last()) {
        '0' -> Color(0xFFE91E63)
        '1' -> Color(0xFF9C27B0)
        '2' -> Color(0xFF673AB7)
        '3' -> Color(0xFF3F51B5)
        '4' -> Color(0xFF2196F3)
        '5' -> Color(0xFF03A9F4)
        '6' -> Color(0xFF00BCD4)
        '7' -> Color(0xFF009688)
        '8' -> Color(0xFF4CAF50)
        '9' -> Color(0xFF8BC34A)
        'A' -> Color(0xFFCDDC39)
        'B' -> Color(0xFFFFEB3B)
        'C' -> Color(0xFFFFC107)
        'D' -> Color(0xFFFF9800)
        'E' -> Color(0xFFFF5722)
        'F' -> Color(0xFF795548)
        'G' -> Color(0xFF9E9E9E)
        'H' -> Color(0xFF607D8B)
        'J' -> Color(0xFF9FA8DA)
        'K' -> Color(0xFF90CAF9)
        'M' -> Color(0xFF81D4FA)
        'N' -> Color(0xFF80DEEA)
        'P' -> Color(0xFF80CBC4)
        'Q' -> Color(0xFFA5D6A7)
        'R' -> Color(0xFFC5E1A5)
        'S' -> Color(0xFFE6EE9C)
        'T' -> Color(0xFFFFF59D)
        'V' -> Color(0xFFFFE082)
        'W' -> Color(0xFFFFCC80)
        'X' -> Color(0xFFFFAB91)
        'Y' -> Color(0xFFFF8A65)
        'Z' -> Color(0xFFFF8A80)
        else -> Color(0xFFFFFFFF)
    }
}