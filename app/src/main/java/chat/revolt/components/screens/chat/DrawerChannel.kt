package chat.revolt.components.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.revolt.api.schemas.ChannelType
import chat.revolt.R

@Composable
fun DrawerChannel(
    channelType: ChannelType,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        when (channelType) {
            ChannelType.TextChannel -> {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(R.drawable.ic_pound_24dp),
                    contentDescription = stringResource(R.string.channel_text)
                )
            }
            ChannelType.VoiceChannel -> {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(R.drawable.ic_volume_up_24dp),
                    contentDescription = stringResource(R.string.channel_voice)
                )
            }
            ChannelType.SavedMessages -> {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(R.drawable.ic_note_24dp),
                    contentDescription = stringResource(R.string.channel_notes)
                )
            }
            else -> {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    imageVector = Icons.Default.List,
                    contentDescription = "Channel"
                )
            }
        }
        Text(
            text = name,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}